`default_nettype none

module ALU 
(
    input wire[3:0] opCode,
    input wire[15:0] oprandA,
    input wire[15:0] oprandB,
    output reg[15:0] result,
    output reg flagOV 
);

    // NOTE: OpCodes
    parameter
        OP_ADD = 4'd1,
        OP_SUB = 4'd2,
        OP_AND = 4'd3,
        OP_OR  = 4'd4,
        OP_XOR = 4'd5,
        OP_NOT = 4'd6,
        OP_SLL = 4'd7,
        OP_SRL = 4'd8,
        OP_SRA = 4'd9,
        OP_ROL = 4'd10;

    wire[15:0] rolResult;
    ALU_ROL (oprandA, oprandB[3:0], rolResult);

        // NOTE: ALU Logics
    always @(*) begin    
        case(opCode)
            OP_ADD : begin
                {flagOV, result} = oprandA + oprandB;
            end
            OP_SUB : begin
                {flagOV, result} = oprandA - oprandB;
            end
            OP_ADD : begin
                result = oprandA & oprandB;
            end
            OP_OR  : begin
                result = oprandA | oprandB;
            end
            OP_XOR : begin
                result = oprandA ^ oprandB;
            end
            OP_NOT : begin
                result = ~oprandA;
            end      
            OP_SLL : begin
                result = oprandA << oprandB[3:0];
            end
            OP_ROL : begin
                // TODO: Rotation
                result = rolResult;
            end
            OP_SRL : begin
                result = oprandA >> oprandB[3:0]; 
            end
            OP_SRA : begin
                // TODO: Shift Right Arithmeticly
                result = oprandA >>> oprandB[3:0];
            end
            default: begin
                // TODO: fill smth
            end
            // TODO: Cauculate Flags SF and VF
        endcase
    end
    // ALU

endmodule  //ALU


module ALU_ROL
(
    input wire[15:0] oprandA,
    input wire[3:0]  oprandB,
    output reg[15:0] out
);

    always @(*) begin
        case (oprandB)
            4'h0 : out = oprandA;
            4'h1 : out = {oprandA[14:0], oprandA[15:15]};
            4'h2 : out = {oprandA[13:0], oprandA[15:14]};
            4'h3 : out = {oprandA[12:0], oprandA[15:13]};
            4'h4 : out = {oprandA[11:0], oprandA[15:12]};
            4'h5 : out = {oprandA[10:0], oprandA[15:11]};
            4'h6 : out = {oprandA[09:0], oprandA[15:10]};
            4'h7 : out = {oprandA[08:0], oprandA[15:09]};
            4'h8 : out = {oprandA[07:0], oprandA[15:08]};
            4'h9 : out = {oprandA[06:0], oprandA[15:07]};
            4'hA : out = {oprandA[05:0], oprandA[15:06]};
            4'hB : out = {oprandA[04:0], oprandA[15:05]};
            4'hC : out = {oprandA[03:0], oprandA[15:04]};
            4'hD : out = {oprandA[02:0], oprandA[15:03]};
            4'hE : out = {oprandA[01:0], oprandA[15:02]};
            4'hF : out = {oprandA[00:0], oprandA[15:01]};
            default: out = 16'hFFFF;
        endcase
    end 

endmodule