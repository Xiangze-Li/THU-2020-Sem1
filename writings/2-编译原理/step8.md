<h1 style="text-align:center">编译原理 MiniDecaf 编译器实验<br/><big>Step 8 实验报告</big></h1>
<p style="text-align:right">李祥泽<br/><small>2018011331<br/>lixiangz18@mails.tsinghua.edu.cn</small></p>

## 实验内容 ##

### 实现循环语句 ###

按实验指导书的指导完成. 参考了助教的示例代码. 

## 思考题 ##

### 从执行的指令的条数这个角度 (`label` 指令不计算在内, 假设循环体至少执行了一次), 请评价这两种翻译方式哪一种更好?  ###

假定 `cond` 和 `body` 的 IR 都算作 1 条指令, 循环体执行 $n$ 次. 

第一种执行命令的条数是 $4n+2$, 即循环被完整执行 $n$ 次, 另加导致循环终止的 1 次 `cond` 和 1 次 `beqz`. 

第二种的条数是 $3n+2$, 即循环被完整执行 $n$ 次, 另加进入循环的 1 次 `cond` 和 1 次 `beqz`. 

仅从指令条数来看, 第二种是更好的.


## Honor Code ##

主要参考实验指导书实现. 较多参考了助教的示例代码. 
