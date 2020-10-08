<template>
  <div id="message-board">
    <el-container style="height: 100%; border: 1px solid #eee">
      <el-header style="text-align: right; font-size: 10px">
        <el-button
          style="display: inline-block; margin-right: 15px"
          v-on:click="postDialog.dialogVisible = true"
        >
          <i class="el-icon-edit">发表</i>
        </el-button>
        <el-button
          style="display: inline-block; margin-right: 15px"
          v-on:click="refresh"
        >
          <i class="el-icon-refresh" style="object-fit: fill">刷新</i>
        </el-button>
      </el-header>

      <el-main>
        <MessageList v-bind:messageList="messageList" />
      </el-main>

      <el-footer>@CST2020SE</el-footer>
    </el-container>
    <PostDialog
      v-on:postit="post_it"
      v-on:hide="postDialog.dialogVisible = false"
      v-bind:dialogVisible="postDialog.dialogVisible"
      v-bind:state="state"
    />
    <el-dialog
      style="text-align: center"
      :title="alertDialog.text"
      :visible.sync="alertDialog.dialogVisible"
      width="40%"
    >
    </el-dialog>
  </div>
</template>

<script>
import MessageList from "@/components/MessageList";
import PostDialog from "@/components/PostDialog";
import { getMessage, postMessage } from "@/utils/communication.js";

export default {
  name: "MessageBoard",
  components: {
    MessageList,
    PostDialog,
  },
  // 请在下方设计自己的数据结构及函数来完成最终的留言板功能
  data() {
    return {
      postDialog: {
        dialogVisible: false,
        form: {
          title: "",
          content: "",
        },
      },
      alertDialog: {
        text: "",
        dialogVisible: false,
      },
      state: {
        username: this.getcookie("user"),
        username_valid: false,
      },
      messageList: [],
    };
  },
  methods: {
    refresh: function () {
      this.messageList = getMessage()
        .then((res) => {
          this.messageList = res.data.data;
          this.messageList.forEach((element) => {
            element.timestamp *= 1000;
          });
        })
        .catch((e) => {
          this.alertDialog.text = "刷新失败";
          this.alertDialog.dialogVisible = true;
          console.log(e);
        });
    },
    post_it: function (message) {
      this.postDialog.dialogVisible = false;
      document.cookie = "user=" + message.user;
      postMessage(message)
        .then(() => {
          this.postDialog.form.title="";
          this.postDialog.form.content="";
          this.alertDialog.text = "发帖成功";
          this.alertDialog.dialogVisible = true;
          this.refresh();
        })
        .catch((e) => {
          this.alertDialog.text = "发帖失败";
          this.alertDialog.dialogVisible = true;
          console.log(e);
        });
    },
    getcookie: function (key) {
      key += "=";
      let cookieEntries = document.cookie.split(";");
      for (let i = 0; i < cookieEntries.length; i++) {
        let entry = cookieEntries[i];
        entry = entry.trim();
        if (entry.indexOf(key) == 0) {
          return entry.substring(key.length);
        }
      }
      return "";
    },
  },
};
</script>

<style scoped>
#message-board {
  height: calc(100vh - 16px);
}
.message-tab {
  display: block;
  padding: 10px;
  text-align: left;
}
.el-header {
  background-color: #b3c0d1;
  color: #333;
  line-height: 60px;
}
.el-footer {
  background-color: #b3c0d1;
  color: #333;
  line-height: 60px;
}
.el-aside {
  color: #333;
}
</style>
