`timescale 1ns / 1ps
`default_nettype none

module Decoder
(
    input wire [31:0]   inst,
    input wire          flagZ,
    input wire [2:0]    stage,

    output reg          pcWr,
    output reg          pcNowWr,
    output reg          pcSel,
    output reg          memSel,
    output reg          memWr,
    output reg          memRd,
    output reg          irWr,
    output reg [2:0]    immSel,
    output reg          regWr,
    output reg [1:0]    regDSel,
    output reg [1:0]    aluASel,
    output reg [1:0]    aluBSel,
    output reg [2:0]    aluOp,
    output reg          aluAlter,

    output reg [2:0]    stageNext
);

    parameter [2:0]
    // Stages
        IF1 = 3'b000,
        IF2 = 3'b001,
        ID  = 3'b010,
        EXE = 3'b011,
        ME1 = 3'b100,
        ME2 = 3'b101,
        WB  = 3'b110,
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

    wire [6:0]  opCode, funct7;
    wire [2:0]  funct3;

    assign opCode = inst[6:0];
    assign funct3 = inst[14:12];
    assign funct7 = inst[31:25];

    parameter [2:0]
        IMM_N = 3'b000,
        // immSel
        IMM_I = 3'b001,
        IMM_S = 3'b010,
        IMM_B = 3'b011,
        IMM_U = 3'b100,
        IMM_J = 3'b101;

    always @(*) begin
        case (stage)
        // 每个阶段为当前阶段准备控制信号!
            IF1 : begin
                pcWr        <= 1'b0;
                pcNowWr     <= 1'b0;
                pcSel       <= 1'b1;
                memSel      <= 1'b0;
                memWr       <= 1'b0;
                memRd       <= 1'b1;
                irWr        <= 1'b1;
                regDSel     <= 2'b11;
                regWr       <= 1'b0;
                immSel      <= IMM_N;
                aluASel     <= 2'b00;
                aluBSel     <= 2'b00;
                aluOp       <= 3'b000;
                aluAlter    <= 1'b0;
            end
            IF2 : begin
                pcWr        <= 1'b1;
                pcNowWr     <= 1'b1;
                pcSel       <= 1'b1;
                memSel      <= 1'b0;
                memWr       <= 1'b0;
                memRd       <= 1'b0;
                irWr        <= 1'b0;
                regDSel     <= 2'b11;
                regWr       <= 1'b0;
                immSel      <= IMM_N;
                aluASel     <= 2'b00;
                aluBSel     <= 2'b00;
                aluOp       <= 3'b000;
                aluAlter    <= 1'b0;
            end
            ID : begin
                pcWr        <= 1'b0;
                pcNowWr     <= 1'b0;
                pcSel       <= 1'b1;
                memSel      <= 1'b0;
                memWr       <= 1'b0;
                memRd       <= 1'b0;
                irWr        <= 1'b0;
                regDSel     <= 2'b11;
                regWr       <= 1'b0;
                immSel      <= IMM_B;
                aluASel     <= 2'b01;
                aluBSel     <= 2'b10;
                aluOp       <= 3'b000;
                aluAlter    <= 1'b0;
            end
            EXE : begin
                case (opCode)
                    OP_R : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_N;
                        aluASel     <= 2'b10;
                        aluBSel     <= 2'b01;
                        aluOp       <= funct3;
                        aluAlter    <= funct7[5];
                    end
                    OP_I : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_I;
                        aluASel     <= 2'b10;
                        aluBSel     <= 2'b10;
                        aluOp       <= funct3;
                        aluAlter    <= 1'b0;
                    end
                    OP_L : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_I;
                        aluASel     <= 2'b10;
                        aluBSel     <= 2'b10;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'b0;
                    end
                    OP_S : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_S;
                        aluASel     <= 2'b10;
                        aluBSel     <= 2'b10;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'b0;
                    end
                    OP_B : begin
                        pcWr        <= flagZ;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b0;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        immSel      <= IMM_B;
                        regWr       <= 1'b0;
                        aluASel     <= 2'b10;
                        aluBSel     <= 2'b01;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'b1;
                    end
                    /*
                    OP_JAL :
                    OP_JALR :
                    OP_LUI :
                    OP_AUIPC :
                    */
                    default : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'bX;
                        memSel      <= 1'bX;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'bXX;
                        regWr       <= 1'b0;
                        immSel      <= 3'bXXX;
                        aluASel     <= 2'bXX;
                        aluBSel     <= 2'bXX;
                        aluOp       <= 3'bXXX;
                        aluAlter    <= 1'bX;
                    end
                endcase
            end
            ME1 : begin
                case (opCode)
                    OP_S : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b1;
                        memWr       <= 1'b1;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_S;
                        aluASel     <= 2'b10;
                        aluBSel     <= 2'b10;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'b0;
                    end
                    OP_L : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b1;
                        memWr       <= 1'b0;
                        memRd       <= 1'b1;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_I;
                        aluASel     <= 2'b10;
                        aluBSel     <= 2'b10;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'b0;
                    end
                    default : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_N;
                        aluASel     <= 2'b11;
                        aluBSel     <= 2'b11;
                        aluOp       <= 3'bXXX;
                        aluAlter    <= 1'bX;
                    end
                endcase
            end
            ME2 : begin
                case (opCode)
                    OP_S : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b1;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_S;
                        aluASel     <= 2'b10;
                        aluBSel     <= 2'b10;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'b0;
                    end
                    OP_L : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b1;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_I;
                        aluASel     <= 2'b10;
                        aluBSel     <= 2'b10;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'b0;
                    end
                    default : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_N;
                        aluASel     <= 2'b11;
                        aluBSel     <= 2'bXX;
                        aluOp       <= 3'bXXX;
                        aluAlter    <= 1'bX;
                    end
                endcase
            end
            WB : begin
                case (opCode)
                    OP_I, OP_R : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b01;
                        regWr       <= 1'b1;
                        immSel      <= IMM_N;
                        aluASel     <= 2'b11;
                        aluBSel     <= 2'b11;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'b0;
                    end
                    OP_L : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b00;
                        regWr       <= 1'b1;
                        immSel      <= IMM_N;
                        aluASel     <= 2'b11;
                        aluBSel     <= 2'b11;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'b0;
                    end
                    default : begin
                        pcWr        <= 1'b0;
                        pcNowWr     <= 1'b0;
                        pcSel       <= 1'b1;
                        memSel      <= 1'b0;
                        memWr       <= 1'b0;
                        memRd       <= 1'b0;
                        irWr        <= 1'b0;
                        regDSel     <= 2'b11;
                        regWr       <= 1'b0;
                        immSel      <= IMM_N;
                        aluASel     <= 2'bXX;
                        aluBSel     <= 2'bXX;
                        aluOp       <= 3'b000;
                        aluAlter    <= 1'bX;
                    end
                endcase
            end
            default : begin
                pcWr        <= 1'b0;
                pcNowWr     <= 1'b0;
                pcSel       <= 1'b1;
                memSel      <= 1'b0;
                memWr       <= 1'b0;
                memRd       <= 1'b0;
                irWr        <= 1'b0;
                regDSel     <= 2'b11;
                regWr       <= 1'b0;
                immSel      <= IMM_N;
                aluASel     <= 2'bXX;
                aluBSel     <= 2'bXX;
                aluOp       <= 3'bXXX;
                aluAlter    <= 1'bX;
            end
        endcase
    end

    always @(*) begin
    // Next Stage Gen.
        case (stage)
            IF1 :
                stageNext = IF2;
            IF2 :
                stageNext = ID;
            ID  :
                stageNext = EXE;
            EXE : begin
                case (opCode)
                    OP_R, OP_I  : stageNext = WB;
                    OP_JALR     : stageNext = WB;
                    OP_AUIPC    : stageNext = WB;
                    OP_LUI      : stageNext = WB;
                    OP_L, OP_S  : stageNext = ME1;
                    OP_B, OP_JAL: stageNext = IF1;
                    default     : stageNext = ERR;
                endcase
            end
            ME1 :
                stageNext = ME2;
            ME2 : begin
                case (opCode)
                    OP_S : stageNext = IF1;
                    OP_L : stageNext = WB;
                    default: stageNext = ERR;
                endcase
            end
            WB :
                stageNext = IF1;
            default:
                stageNext = ERR;
        endcase
    end

endmodule
