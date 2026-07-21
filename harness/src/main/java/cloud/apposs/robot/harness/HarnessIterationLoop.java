package cloud.apposs.robot.harness;

import cloud.apposs.logger.Logger;
import cloud.apposs.react.IoFunction;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.message.kind.AIAssistantMessage;
import cloud.apposs.robot.harness.message.kind.AIToolMessage;
import cloud.apposs.robot.harness.message.kind.AIUserMessage;
import cloud.apposs.robot.harness.provider.AIRequest;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.provider.AITool;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.robot.harness.util.PromptLoader;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

public class HarnessIterationLoop implements IoFunction<AIResponse, React<AIResponse>> {
    // 返回此值表示终止循环并将当前响应传递给下游订阅方
    private static final React<AIResponse> HARNESS_LOOP_BREAK = null;
    // 返回此值表示跳过当前响应，静默终止循环（不向下游传值）
    private static final React<AIResponse> HARNESS_LOOP_SKIP = React.create(subscriber -> {
    });

    private final HarnessWorker worker;

    private final AIRequest request;

    private final IMessageHook messageHook;

    private int iteration = 0;

    public HarnessIterationLoop(HarnessWorker worker, AIRequest request, IMessageHook messageHook) {
        this.worker = worker;
        this.request = request;
        this.messageHook = messageHook;
    }

    @Override
    public React<AIResponse> call(AIResponse response) throws Exception {
        // 如果响应流尚未结束，不先向下游传递响应结果，记录执行进度
        if (!response.isFinished()) {
            if (messageHook != null) {
                messageHook.onProcessing(request.getSid(), request.getRid(), response);
            }
            return HARNESS_LOOP_SKIP;
        }
        // 如果当前响应没有工具调用，说明智能体没有后续操作需要执行了，直接将结果传递给下游订阅方，终止循环
        if (!response.hasToolCall()) {
            return HARNESS_LOOP_BREAK;
        }
        // 判断是否达到最大迭代次数，终止循环
        HarnessWorkerProfile profile = worker.getProfile();
        int maxIterations = profile.getMaxIterations();
        if (iteration++ >= maxIterations) {
            Logger.warn("Worker harness: max iterations (%d) reached", maxIterations);
            // 如果迭代达到上限，但AI有可能继续调用工具，为避免死循环，进行兜底处理
            if (iteration >= maxIterations + 4) {
                return React.just(AIResponse.of(null, "Maximum iterations reached, stopping the loop.",
                        HttpStatus.HTTP_STATUS_429.getCode(), true));
            }
            return handleIterationLimitBreak();
        }
        // 通知外部当前迭代的工具调用响应，方便展现执行进度
        if (messageHook != null) {
            messageHook.onProcessing(request.getSid(), request.getRid(), response);
        }
        // 调用工具前，当前迭代的响应结果会通过 React 流的 onNext 传递给订阅方
        // 遍历工具列表执行调用，并将结果追加到消息历史，如果工具调用结果中又包含新的工具调用，则继续下一轮循环，直到没有工具调用或者达到最大迭代次数
        Table<AITool> tools = response.getTools();
        // 将工具列表转为有序列表，便于顺序链式调用
        Table<AITool> toolList = Table.builder();
        for (AITool tool : tools) {
            if (tool.getId() == null) {
                Logger.warn("Worker harness: tool [%s] has no id, skipping", tool.getName());
                continue;
            }
            toolList.add(tool);
        }
        // 构建顺序工具调用链逐个串联，最终触发 completions 请求
        AIProviderSetting provider = profile.getPrimaryProvider();
        React<Void> buildedToolChain = handleToolChainBuild(toolList, 0, response, tools);
        Logger.info("Worker harness: iteration %d, calling provider [%s] with tools %s", iteration, provider.getName(), tools);
        // 所有工具执行完毕后，发起下一轮 completions 请求
        return buildedToolChain.request((result) -> {
            return worker.completions(request, provider);
        });
    }

    // 递归构建顺序工具调用链，每个工具执行完毕后将结果追加到消息上下文，再执行下一个工具
    private React<Void> handleToolChainBuild(Table<AITool> toolList, int index, AIResponse response, Table<AITool> tools) throws Exception {
        if (index >= toolList.size()) {
            return React.just(null);
        }
        AITool tool = toolList.get(index);
        Param arguments = JsonUtil.parseJsonParam(tool.getArguments());
        if (arguments == null) {
            return handleInvalidToolArguments(toolList, tool, index, response, tools);
        }
        return handleFunctionRun(request.getWid(), request.getSid(), request.getRid(), tool.getName(), arguments)
                .request(result -> {
                    tool.setSuccess(result == null || !result.startsWith("Error:"));
                    AIMessages messages = request.getMessages();
                    // 第一个工具执行完毕后，先追加本轮响应消息，再追加工具结果
                    if (index == 0) {
                        AIAssistantMessage assistantMessage = new AIAssistantMessage(response.getContent());
                        if (response.hasReasoningContent()) {
                            assistantMessage.setReasoning(response.getReasoningContent());
                        }
                        assistantMessage.setTools(tools);
                        messages.append(assistantMessage, true);
                    }
                    AIToolMessage toolMessage = new AIToolMessage(result);
                    toolMessage.setId(tool.getId());
                    toolMessage.setName(tool.getName());
                    messages.append(toolMessage, true);
                    String logContent = String.format("Tool Call: %s(%s) - %s", tool.getName(), tool.getArguments(), result);
                    worker.getFramework().getLogger().print(request.getWid(), request.getSid(), request.getRid(), logContent);
                    // 继续执行下一个工具
                    return handleToolChainBuild(toolList, index + 1, response, tools);
                });
    }

    private React<Void> handleInvalidToolArguments(Table<AITool> toolList, AITool tool, int index, AIResponse response, Table<AITool> tools) throws Exception {
        String result = "Error: Tool [" + tool.getName() + "] arguments must be a valid JSON object and must not be empty.";
        tool.setSuccess(false);
        AIMessages messages = request.getMessages();
        if (index == 0) {
            AIAssistantMessage assistantMessage = new AIAssistantMessage(response.getContent());
            if (response.hasReasoningContent()) {
                assistantMessage.setReasoning(response.getReasoningContent());
            }
            assistantMessage.setTools(tools);
            messages.append(assistantMessage, true);
        }
        AIToolMessage toolMessage = new AIToolMessage(result);
        toolMessage.setId(tool.getId());
        toolMessage.setName(tool.getName());
        messages.append(toolMessage, true);
        String logContent = String.format("Tool Call: %s(%s) - %s", tool.getName(), tool.getArguments(), result);
        worker.getFramework().getLogger().print(request.getWid(), request.getSid(), request.getRid(), logContent);
        return handleToolChainBuild(toolList, index + 1, response, tools);
    }

    // 构建工具调用数据流链，执行工具调用
    private React<String> handleFunctionRun(String wid, String sid, String rid, String name, Param arguments) {
        ITool function = worker.getToolKit().getTool(name);
        if (function == null) {
            Logger.warn("Worker harness: no matched tool for name [%s]", name);
            return React.just("Error: No matched tool for name [" + name + "]");
        }
        if (!function.validate(arguments)) {
            Logger.warn("Worker harness: tool [%s] validation failed for arguments %s", name, arguments);
            return React.just("Error: Tool [" + name + "] validation failed for arguments " + arguments);
        }
        try {
            return function.run(wid, sid, rid, arguments, messageHook)
                    .map((result) -> {
                        return result != null ? result : "";
                    });
        } catch (Exception e) {
            Logger.error(e, "Worker harness: tool [%s] execution failed", name);
            return React.just("Error: Tool [" + name + "] execution failed: " +
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    // 达到最大迭代次数后的处理逻辑，追加系统消息提示用户，并终止循环
    private React<AIResponse> handleIterationLimitBreak() throws Exception {
        HarnessWorkerProfile profile = worker.getProfile();
        AIMessages messages = request.getMessages();
        Param replacement = Param.builder("maxIterations", profile.getMaxIterations());
        String prompt = PromptLoader.readPrompt("iteration/limit", replacement);
        AIUserMessage userMessage = new AIUserMessage(prompt);
        messages.append(userMessage);
        return worker.completions(request);
    }
}
