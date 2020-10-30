`timescale 1ns / 1ps
`default_nettype none

module RegFile
(
    input wire          clk,
                        rst,

    input wire          regWr,

    input wire[4:0]     rs1,
                        rs2,
                        rd,

    input wire[31:0]    data,

    output reg[31:0]   q1,
                        q2
);

    reg[31:0] Regs[31:0];

    // TODO: 怀疑此处是否能够使用阻塞赋值,
    // TODO: 检查以下代码生成的电路
    always @(posedge clk, posedge rst) begin
        if (rst) begin
            for (integer i = 0; i<32; i=i+1) begin
                Regs[i] = 32'b0;
            end
            q1 = 32'b0;
            q2 = 32'b0;
        end
        else begin
            if (regWr) Regs[rd] = data;
            q1 = Regs[rs1];
            q2 = Regs[rs2];
        end
    end

endmodule
