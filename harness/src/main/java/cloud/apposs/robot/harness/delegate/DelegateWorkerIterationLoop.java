package cloud.apposs.robot.harness.delegate;

import cloud.apposs.logger.Logger;
import cloud.apposs.react.IoFunction;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.HarnessWorker;
import cloud.apposs.robot.harness.bus.IMessageHook;
import cloud.apposs.robot.harness.provider.AIRequest;
import cloud.apposs.robot.harness.provider.AIResponse;
import cloud.apposs.robot.harness.setting.AIProviderSetting;
import cloud.apposs.robot.harness.message.AIMessages;
import cloud.apposs.robot.harness.provider.AITool;
import cloud.apposs.robot.harness.message.kind.AIAssistantMessage;
import cloud.apposs.robot.harness.message.kind.AIToolMessage;
import cloud.apposs.robot.harness.tool.ITool;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

public class DelegateWorkerIterationLoop implements IoFunction<AIResponse, React<AIResponse>> {
    private static final React<AIResponse> HARNESS_LOOP_BREAK = null;
    private static final React<AIResponse> HARNESS_LOOP_SKIP = React.create(subscriber -> {
    });

    private final HarnessWorker worker;

    private final DelegateWorkerProfile profile;

    private final AIRequest request;

    private final IMessageHook messageHook;

    private int iteration = 0;

    public DelegateWorkerIterationLoop(HarnessWorker worker, DelegateWorkerProfile profile, AIRequest request, IMessageHook messageHook) {
        this.worker = worker;
        this.profile = profile;
        this.request = request;
        this.messageHook = messageHook;
    }

    @Override
    public React<AIResponse> call(AIResponse response) throws Exception {
        if (!response.isFinished()) {
            if (messageHook != null) {
                messageHook.onProcessing(request.getSid(), request.getRid(), response);
            }
            return HARNESS_LOOP_SKIP;
        }
        if (!response.hasToolCall()) {
            return HARNESS_LOOP_BREAK;
        }
        int maxIterations = profile.getMaxIterations();
        if (iteration++ >= maxIterations) {
            Logger.warn("Worker harness: max iterations (%d) reached", maxIterations);
            return React.just(AIResponse.of(null, "Maximum iterations reached, stopping the loop.",
                    HttpStatus.HTTP_STATUS_429.getCode(), true));
        }
        if (messageHook != null) {
            messageHook.onProcessing(request.getSid(), request.getRid(), response);
        }
        Table<AITool> tools = response.getTools();
        Table<AITool> toolList = Table.builder();
        for (AITool tool : tools) {
            if (tool.getId() == null) {
                Logger.warn("Worker harness: tool [%s] has no id, skipping", tool.getName());
                continue;
            }
            toolList.add(tool);
        }
        React<Void> buildedToolChain = handleToolChainBuild(toolList, 0, response, tools);
        AIProviderSetting provider = profile.getPrimaryProvider();
        Logger.info("Worker harness: iteration %d, calling provider [%s] with tools %s", iteration, provider.getName(), tools);
        return buildedToolChain.request((result) -> {
            return worker.completions(request, provider);
        });
    }

    private React<Void> handleToolChainBuild(Table<AITool> toolList, int index, AIResponse response, Table<AITool> tools) {
        if (index >= toolList.size()) {
            return React.just(null);
        }
        AITool tool = toolList.get(index);
        Param arguments = JsonUtil.parseJsonParam(tool.getArguments());
        return handleFunctionRun(request.getWid(), request.getSid(), tool.getName(), arguments)
            .request(result -> {
                AIMessages messages = request.getMessages();
                if (index == 0) {
                    AIAssistantMessage assistantMessage = new AIAssistantMessage(response.getContent());
                    assistantMessage.setTools(tools);
                    messages.append(assistantMessage);
                }
                AIToolMessage toolMessage = new AIToolMessage(result);
                toolMessage.setId(tool.getId());
                toolMessage.setName(tool.getName());
                messages.append(toolMessage);
                return handleToolChainBuild(toolList, index + 1, response, tools);
            });
    }

    private React<String> handleFunctionRun(String wid, String sid, String name, Param arguments) {
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
            return function.run(wid, sid, request.getRid(), arguments, messageHook)
                    .map((result) -> {
                        return result != null ? result : "";
                    });
        } catch (Exception e) {
            Logger.error(e, "Worker harness: tool [%s] execution failed", name);
            return React.just("Error: Tool [" + name + "] execution failed: " +
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
