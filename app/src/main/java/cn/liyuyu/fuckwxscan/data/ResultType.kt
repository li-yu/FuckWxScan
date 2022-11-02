package cn.liyuyu.fuckwxscan.data

/**
 * Created by frank on 2022/11/2.
 */
sealed class ResultType {
    object WeChatUrl : ResultType()
    object AlipayUrl : ResultType()
    object CommonUrl : ResultType()
    object PlainText : ResultType()
}
