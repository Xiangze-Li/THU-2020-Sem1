`default_nettype none

module MemController
(
    input wire clk,
    input wire rst,

    output wire[7:0] seg7disp,
    input wire[19:0] base_addr_in,

    inout wire[31:0] data,

    output wire[19:0] ram_addr,
    output wire[3:0] ram_be_n,
    output reg ram_oe_n,
    output reg ram_we_n,

    input wire uart_dataready,
    input wire uart_tbre,
    input wire uart_tsre,
    output reg uart_rdn,
    output reg uart_wrn
);

    assign ram_be_n =

    reg[1:0] mainState;
    localparam
        MAIN_STDBY = 2'b00,
        MAIN_READ  = 2'b01,
        MAIN_WRITE = 2'b10,
        MAIN_IDLE  = 2'b11;
    reg[3:0] mainCntr;

    reg[2:0] readState;
    localparam
        READ_STDBY      = 3'b000,
        READ_WAIT_UART  = 3'b001,
        READ_READ_UART  = 3'b010,
        READ_BUBBLE     = 3'b011,
        READ_WAIT_RAM   = 3'b100,
        READ_WRITE_RAM  = 3'b101,
        READ_IDLE       = 3'b110;

    reg[2:0] writeState;
    localparam
        WRITE_STDBY      = 3'b000,
        WRITE_WAIT_RAM   = 3'b001,
        WRITE_READ_RAM   = 3'b010,
        WRITE_BUBBLE     = 3'b011,
        WRITE_WRITE_UART = 3'b100,
        WRITE_WAIT_TBRE  = 3'b101,
        WRITE_WAIT_TSRE  = 3'b110,
        WRITE_IDLE       = 3'b111;

    reg startSig;

    reg[19:0] baseAddr;
    assign ram_addr = baseAddr + mainCntr;

    wire[1:0] isIdle;
    assign isIdle = { readState == READ_IDLE, writeState == WRITE_IDLE };

    // State Machine MAIN
    always @(posedge rst or posedge clk) begin
        if (rst) begin
            mainState  <= MAIN_STDBY;
            readState  <= READ_IDLE;
            writeState <= WRITE_IDLE;
            baseAddr   <= base_addr_in;
            mainCntr   <= 4'h0;
            // TODO : 把所有使能都关掉
        end
        else begin
            case (isIdle)
                2'b11 : begin
                    case (mainState)
                        MAIN_STDBY : begin

                        end
                        MAIN_READ  :
                        MAIN_WRITE :
                        MAIN_IDLE  :
                        default    :
                    endcase
                end
                2'b01 : begin
                    // TODO : 读串口, 写内存
                    case (readState)
                        READ_STDBY      : begin
                            data <= 32'bz;
                            readState <= READ_WAIT_UART;
                        end
                        READ_WAIT_UART  : begin
                            if (uart_dataready) begin
                                uart_rdn <= 0;
                                readState <= READ_READ_UART;
                            end
                        end
                        READ_READ_UART  :
                        READ_BUBBLE     :
                        READ_WAIT_RAM   :
                        READ_WRITE_RAM  :
                    endcase
                end
                2'b10 : begin
                    // TODO : 读内存, 写串口
                end
                2'b00 : begin
                    // 跑飞了
                    mainState  <= MAIN_IDLE;
                    readState  <= READ_IDLE;
                    writeState <= WRITE_IDLE;
                end
            end
            else if (readState != READ_IDLE && writeState != WRITE_IDLE)
        end
    end



endmodule
