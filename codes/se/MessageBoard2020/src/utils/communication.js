/**
 * 如果需要修改为正常上线模式，请注释下面mock的import代码
 * **/
// import "@/mock/index";
import API from "./API";
import axios from "axios";

// 请在下方实现自己的后端通信函数

export function getMessage() {
    return axios.get(API.GET_MESSAGE_LIST.path);
}

export function postMessage(message) {
    return axios.post(API.POST_NEW_MESSAGE.path, {
        title: message.title,
        content: message.content,
    });
}
