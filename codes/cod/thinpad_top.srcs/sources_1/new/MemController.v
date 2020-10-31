`default_nettype none

module MemController
(
    input  wire         clk,

    input  wire [31:0]  baseDIn,
    output reg  [31:0]  baseDOut,
    input  wire [31:0]  baseA,
    input  wire         baseWr,
    input  wire         baseRd,

    inout  wire [31:0]  baseData,
    output wire [19:0]  baseAddr,
    output reg          baseCeN,
    output reg  [3:0]   baseBeN,
    output reg          baseOeN,
    output reg          baseWeN,

    inout  wire [31:0]  extData,
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

    assign uartRdN = 1'b1;
    assign uartWrN = 1'b1;

    assign extCeN = 1'b1;
    assign extBeN = 4'b1111;
    assign extOeN = 1'b1;
    assign extWeN = 1'b1;


endmodule
