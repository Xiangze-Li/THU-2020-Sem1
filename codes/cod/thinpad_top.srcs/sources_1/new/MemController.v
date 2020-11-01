`default_nettype none

module MemController
(
    input  wire         clk,
    input  wire         rst,

    input  wire [31:0]  baseDIn,
    output reg  [31:0]  baseDOut,
    input  wire [31:0]  baseA,
    input  wire         baseWr,
    input  wire         baseRd,

    inout  wire [31:0]  baseIO,
    output wire [19:0]  baseAddr,
    output wire         baseCeN,
    output wire [3:0]   baseBeN,
    output wire         baseOeN,
    output wire         baseWeN,

    inout  wire [31:0]  extIO,
    output wire [19:0]  extAddr,
    output wire         extCeN,
    output wire [3:0]   extBeN,
    output wire         extOeN,
    output wire         extWeN,

    input  wire         uartDataready,
    input  wire         uartTbrE,
    input  wire         uartTsrE,
    output wire         uartRdN,
    output wire         uartWrN
);

    localparam [2:0]
        S_IDLE = 3'b000,
        S_RD_1 = 3'b001,
        S_RD_2 = 3'b010,
        S_WR_1 = 3'b011,
        S_WR_2 = 3'b100,
        S_DONE = 3'b111;
    reg [2:0] state;

    assign uartRdN = 1'b1;
    assign uartWrN = 1'b1;

    assign extCeN = 1'b1;
    assign extBeN = 4'b1111;
    assign extOeN = 1'b1;
    assign extWeN = 1'b1;
    assign extIO  = 32'bZ;
    assign extAddr = 20'b0;

    reg baseCeN_R, baseOeN_R, baseWeN_R, baseZ;
    reg [31:0] baseData;

    assign baseCeN = 1'b0;
    assign baseBeN = 4'b0000;
    assign baseOeN = baseOeN_R;
    assign baseWeN = baseWeN_R;
    assign baseAddr = baseA[21:2];
    assign baseIO   = baseZ ? 32'bZ : baseData;

    always @(posedge clk, posedge rst) begin
        if (rst) begin
            state <= S_IDLE;
            baseZ <= 1'b1;
            baseCeN_R <= 1'b1;
            baseOeN_R <= 1'b1;
            baseWeN_R <= 1'b1;
        end
        else begin
            case (state)
                S_IDLE : begin
                    if (baseRd) begin
                        baseZ       <= 1'b1;
                        state       <= S_RD_1;
                    end
                    else if (baseWr) begin
                        baseZ       <= 1'b0;
                        baseData    <= baseDIn;
                        state       <= S_WR_1;
                    end
                end
                S_RD_1 : begin
                    state       <= S_RD_2;
                    baseCeN_R   <= 1'b0;
                    baseOeN_R   <= 1'b0;
                end
                S_RD_2 : begin
                    state       <= S_DONE;
                    baseCeN_R   <= 1'b1;
                    baseOeN_R   <= 1'b1;
                    baseDOut    <= baseIO;
                end
                S_WR_1 : begin
                    state       <= S_WR_2;
                    baseCeN_R   <= 1'b0;
                    baseWeN_R   <= 1'b0;
                end
                S_WR_2 : begin
                    state       <= S_DONE;
                    baseCeN_R   <= 1'b1;
                    baseWeN_R   <= 1'b1;
                end
                S_DONE : begin
                    baseZ       <= 1'b1;
                    if (~baseWr & ~baseRd)
                        state   <= S_IDLE;
                end
                default: begin

                end
            endcase
        end
    end



endmodule
