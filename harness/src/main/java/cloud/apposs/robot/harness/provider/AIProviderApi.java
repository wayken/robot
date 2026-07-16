package cloud.apposs.robot.harness.provider;

import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.react.React;
import cloud.apposs.robot.harness.setting.AIProviderSetting;

/**
 * AI模型调用接口，实现该接口以支持不同AI模型服务商的接入，提供统一的调用方式和参数规范
 */
public interface AIProviderApi {
    /**
     * 发送AI请求，获取响应
     *
     * @param  httpClient Http客户端实例
     * @param  provider   AI模型服务商
     * @param  request    AI请求参数封装，包括消息列表、工具列表等
     * @return 包含AI模型响应内容的React对象
     */
    React<AIResponse> completions(OkHttp httpClient, AIProviderSetting provider, AIRequest request) throws Exception;
}
