<h1 style="text-align:center">编译原理 MiniDecaf 编译器实验<br/><big>Step 10 实验报告</big></h1>
<p style="text-align:right">李祥泽<br/><small>2018011331<br/>lixiangz18@mails.tsinghua.edu.cn</small></p>

## 实验内容 ##

### 实现全局变量 ###

大致按实验指导书的指导完成. 

## 思考题 ##

### 请给出将全局变量 `a` 的值读到寄存器 `t0` 所需的 RISC-V 指令序列.  ###

```asm
la	t0, a
lw	t0, 0(t0)
```

使用伪指令 `la` 就可以取得全局变量的地址, 然后对相应地址访存. 

## Honor Code ##

主要参考实验指导书实现. 参考了助教的示例代码.
