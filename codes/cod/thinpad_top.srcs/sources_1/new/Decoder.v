`timescale 1ns / 1ps
`default_nettype none

module Decoder
(
    input wire[31:0]    inst,
    input wire          flagZ,
    input wire[2:0]     stage,

    output reg          pcWr,
    output reg          pcNowWr,
    output reg          pcSel,
    output reg          memSel,
    output reg          memWr,
    output reg          memRd,
    output reg          irWr,
    output reg          mem2Reg,
    output reg[2:0]     immSel,
    output reg          regWr,
    output reg          aluSelA,
    output reg          aluSelB,
    output reg          aluOp,
    output reg          aluAlter,

    output reg[2:0]     stageNext
);

    parameter [2:0]
    // Stage
        IF = 3'b001,
        ID = 3'b010,
        EX = 3'b011,
        ME = 3'b100,
        WB = 3'b101,
        ERR = 3'b111;

    parameter [6:0]
    // OpCode
        OP_R        = 7'b0110011,
        OP_I        = 7'b0010011,
        OP_S        = 7'b0100011,
        OP_L        = 7'b0000011,
        OP_B        = 7'b1100011,
        // opcodes below not used in exp-5
        OP_JALR     = 7'b1100111,
        OP_JAL      = 7'b1101111,
        OP_AUIPC    = 7'b0010111,
        OP_LUI      = 7'b0110111;

    parameter [2:0]
        // immSel
        IMM_I = 3'b001,
        IMM_S = 3'b010,
        IMM_B = 3'b011,
        IMM_U = 3'b100,
        IMM_J = 3'b101;

    always @(*) begin
    // Countrol Sig. Gen.
        case (inst[6:0])
            OP_R    : begin
                case (stage)
                    IF :
                    ID :
                    EX :
                    ME :
                    WB :
                    default:
                endcase
            end
            OP_I    : begin
                case (stage)
                    IF :
                    ID :
                    EX :
                    ME :
                    WB :
                    default:
                endcase
            end
            OP_S    : begin
                case (stage)
                    IF :
                    ID :
                    EX :
                    ME :
                    WB :
                    default:
                endcase
            end
            OP_L    : begin
                case (stage)
                    IF :
                    ID :
                    EX :
                    ME :
                    WB :
                    default:
                endcase
            end
            OP_B    : begin
                case (stage)
                    IF :
                    ID :
                    EX :
                    ME :
                    WB :
                    default:
                endcase
            end
            // OP_JAL  :
            // OP_JALR :
            // OP_AUIPC:
            // OP_LUI  :
            default:
        endcase
    end

    always @(*) begin
    // Next Stage Gen.
        case (stage)
            IF : stageNext = ID;
            ID : stageNext = EX;
            EX : begin
                case (inst[6:0])
                    OP_R, OP_I  : stageNext = WB;
                    OP_JALR     : stageNext = WB;
                    OP_AUIPC    : stageNext = WB;
                    OP_LUI      : stageNext = WB;
                    OP_L, OP_S  : stageNext = ME;
                    OP_B, OP_JAL: stageNext = IF;
                    default     : stageNext = ERR;
                endcase
            end
            ME : begin
                case (inst[6:0])
                    OP_S : stageNext = IF;
                    OP_L : stageNext = WB;
                    default: stageNext = ERR;
                endcase
            end
            WB : stageNext = IF;
            default:
        endcase
    end

endmodule
