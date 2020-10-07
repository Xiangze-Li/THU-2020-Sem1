<template>
  <el-dialog
    style="text-align: center"
    title="发表"
    :visible.sync="dialogVisible"
    :show-close="false"
    width="80%"
  >
    <el-form label-width="80px">
      <el-form-item label="标题">
        <el-input v-model="title" placeholder="title"></el-input>
      </el-form-item>
      <el-form-item label="内容">
        <el-input
          v-model="content"
          type="textarea"
          placeholder="message"
        ></el-input>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="state.username"></el-input>
        <span v-if="state.username_valid === false" style="color: red"
          >请设置合法用户名!</span
        >
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="onCancel">取 消</el-button>
      <el-button
        type="primary"
        :disabled="state.username_valid === false"
        @click="onConfirm"
        >确 定</el-button
      >
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: "PostDialog",
  props: {
    dialogVisible: {
      type: Boolean,
      default: () => true,
    },
    state: {
      type: Object,
      default: () => {
        return {
          username: "",
          username_valid: false,
        };
      },
    },
  },
  // 请在下方设计自己的数据结构以及事件函数
  data() {
    return {
      title: "",
      content: "",
    };
  },
  methods: {
    onConfirm: function () {
      let message = {
        title: this.title,
        content: this.content,
        user: this.state.username,
        timestamp: new Date().getTime(),
      };
      this.$emit("postit", message);
    },
    onCancel: function () {
      this.$emit("hide");
    },
  },
  watch: {
    // 用于实时检测username是否合法
    "state.username": {
      handler(newName) {
        this.state.username_valid = /^[A-Za-z\u4e00-\u9fa5][-A-Za-z0-9\u4e00-\u9fa5_]*$/.test(
          newName
        );
      },
    },
  },
};
</script>
