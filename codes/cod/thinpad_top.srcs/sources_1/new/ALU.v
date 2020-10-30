`default_nettype none

module ALU
(
    input wire[2:0] opCode,
    input wire alter,
    input signed wire[31:0] oprandA,
    input signed wire[31:0] oprandB,
    output signed reg[31:0] result,
    output wire flagZero
);

    // - NOTE: OpCodes, same definition as <func3>
    //         while alter is <func7[5]>, or inst[30]
    parameter [2:0]
        ADD  = 3'b000,
    //  SUB  = 3'b000 with alter
        SLL  = 3'b001,
        SLT  = 3'b010,
        SLTU = 3'b011,
        XOR  = 3'b100,
        SRL  = 3'b101,
    //  SRA  = 3'b101 with alter
        OR   = 3'b110,
        AND  = 3'b111;

    assign flagZero = &(~result);

    // - NOTE: ALU Logics
    always @(*) begin
        case(opCode)
            ADD  : result = oprandA + (alter ? ~oprandB + 1 : oprandB);
            // SLL  : result = oprandA << oprandB[4:0];
            // SLT  : result = (oprandA < oprandB ? 32'b1 : 32'b0);
            // SLTU : result = ($unsigned(oprandA) < $unsigned(oprandB) ? 32'b1 : 32'0);
            // XOR  : result = oprandA ^ oprandB;
            // SRL  : result = (alter ? oprandA >> oprandB[4:0] : oprandA >>> oprandB[4:0]);
            OR   : result = oprandA | oprandB;
            AND  : result = oprandA & oprandB;
            default: result = 32'b0;
        endcase
    end

endmodule  //ALU
