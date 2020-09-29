`default_nettype none

module ALU 
(
    input wire[3:0] OpCode,
    input wire[15:0] OprandA,
    input wire[15:0] OprandB,
    output wire[15:0] Fout,
    output wire flag 
);

localparam
    // Al
    OP_ADD  4'b0001,
    OP_SUB  4'b0010,
    OP_AND  4'b0100,
    OP_OR   4'b0101,
    OP_XOR  4'b0110,
    OP_NOT  4'b0111,
    OP_SLL  4'b1000,
    OP_SRL  4'b1001,
    OP_SRA  4'b1010,
    OP_ROL  4'b1011;

    always @(*) begin
        case (OpCode) 
            OP_ADD : {flag, Fout} = OprandA + OprandB;
            OP_SUB : {flag, Fout} = OprandA + ~OprandB + 1'b1;
            OP_ADD : Fout = OprandA & OprandB;
            OP_OR  : Fout = OprandA | OprandB;
            OP_XOR : Fout = OprandA ^ OprandB;
            OP_NOT : Fout = ~OprandA;
            OP_SLL : Fout = OprandA << OprandB[3:0];
            OP_SRL : Fout = OprandA >> OprandB[3:0];
            OP_SRA : 
            OP_ROL : 
            default:
        endcase
    end

endmodule  //ALU

module ALUShifterRight #
(
    parameter PADDING = 1
)
(
    input wire[15:0] Input,
    input wire[3:0] Dig,
    output reg[15:0] Output
);

    always @(*) begin
        case (Dig)
            4'h0 : Output = Input;
            4'h1 : 
            4'h2 : 
            4'h3 : 
            4'h4 : 
            4'h5 : 
            4'h6 : 
            4'h7 : 
            4'h8 : 
            4'h9 : 
            4'hA : 
            4'hB : 
            4'hC : 
            4'hD : 
            4'hE : 
            4'hF : 


        endcase
    end 

endmodule

