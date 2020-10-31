`timescale 1ns / 1ps
`default_nettype none

module RegFile
(
    input wire          clk,

    input wire          regWr,

    input wire[4:0]     rs1,
                        rs2,
                        rd,

    input wire[31:0]    data,

    output wire[31:0]   q1,
                        q2
);

    reg[31:0] Regs[31:0];


    assign q1 = Regs[rs1];
    assign q2 = Regs[rs2];

    // TODO: 怀疑此处是否能够使用阻塞赋值,
    // TODO: 检查以下代码生成的电路
    always @(posedge clk) begin
        if (regWr) Regs[rd] = data;
    end

endmodule
