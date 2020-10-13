`default_nettype none

module MemController
(
    input wire clk,
    input wire rst,

    output wire[7:0] seg7disp,
    input wire[19:0] base_addr_in,

    inout wire[31:0] ram_data,

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

    reg[3:0] writeState;
    localparam
        WRITE_STDBY      = 4'b0000,
        WRITE_WAIT_RAM   = 4'b0001,
        WRITE_READ_RAM   = 4'b0010,
        WRITE_BUBBLE     = 4'b0011,
        WRITE_LOAD_UART  = 4'b0100,
        WRITE_WRITE_UART = 4'b0101,
        WRITE_WAIT_TBRE  = 4'b0110,
        WRITE_WAIT_TSRE  = 4'b0111,
        WRITE_IDLE       = 4'b1000;

    reg[19:0] baseAddr;
    assign ram_addr = baseAddr + mainCntr;

    assign ram_be_n = 4'b0000;

    wire[1:0] isIdle;
    assign isIdle = { readState == READ_IDLE, writeState == WRITE_IDLE };

    reg[31:0] data;

    // State Machine
    always @(posedge rst or posedge clk) begin
        if (rst) begin
            mainState  <= MAIN_STDBY;
            readState  <= READ_IDLE;
            writeState <= WRITE_IDLE;
            baseAddr   <= base_addr_in;
            mainCntr   <= 4'h0;
            // + TODO : 把所有使能都关掉
            ram_we_n   <= 1'b1;
            ram_oe_n   <= 1'b1;
            uart_rdn   <= 1'b1;
            uart_wrn   <= 1'b1;
        end
        else begin
            case (isIdle)
                2'b11 : begin
                    case (mainState)
                        // TODO : 主状态机转移
                        MAIN_STDBY : begin
                            mainCntr  <= 4'h0;
                            mainState <= MAIN_READ;
                        end
                        MAIN_READ  : begin
                            if (mainCntr == 4'hA) begin
                                mainCntr  <= 4'h0;
                                mainState <= MAIN_WRITE;
                            end
                            else begin
                                mainCntr  <= mainCntr + 1;
                                readState <= READ_STDBY;
                            end
                        end
                        MAIN_WRITE : begin
                            if (mainCntr == 4'hA) begin
                                mainCntr  <= 4'h0;
                                mainState <= MAIN_IDLE;
                            end
                            else begin
                                mainCntr  <= mainCntr + 1;
                                writeState <= WRITE_STDBY;
                            end
                        end
                        MAIN_IDLE  : begin
                            mainState <= MAIN_IDLE;
                        end
                        default    : // shouldn't
                    endcase
                end
                2'b01 : begin
                    // + TODO : 读串口, 写内存
                    case (readState)
                        READ_STDBY     : begin
                            data      <= 32'bz;
                            readState <= READ_WAIT_UART;
                        end
                        READ_WAIT_UART : begin
                            if (uart_dataready) begin
                                uart_rdn  <= 1'b0;
                                readState <= READ_READ_UART;
                            end
                        end
                        READ_READ_UART : begin
                            data      <= ram_data;
                            uart_rdn  <= 1'b1;
                            readState <= READ_BUBBLE;
                        end
                        READ_BUBBLE    : begin
                            ram_data  <= data;
                            readState <= READ_WAIT_RAM;
                        end
                        READ_WAIT_RAM  : begin
                            ram_we_n  <= 1'b0;
                            readState <= READ_WRITE_RAM;
                        end
                        READ_WRITE_RAM : begin
                            ram_we_n  <= 1'b1;
                            readState <= READ_IDLE;
                        end
                        default        : begin
                            // 跑飞了
                            readState <= READ_IDLE;
                            ram_we_n  <= 1'b1;
                            uart_rdn  <= 1'b1;
                        end
                    endcase
                end
                2'b10 : begin
                    // + TODO : 读内存, 写串口
                    case (writeState)
                        WRITE_STDBY      : begin
                            writeState <= WRITE_WAIT_RAM;
                            ram_data   <= 32'bz;
                        end
                        WRITE_WAIT_RAM   : begin
                            writeState <= WRITE_READ_RAM;
                            ram_oe_n   <= 1'b0;
                        end
                        WRITE_READ_RAM   : begin
                            writeState <= WRITE_BUBBLE;
                            ram_oe_n   <= 1'b1;
                            data       <= ram_data;
                        end
                        WRITE_BUBBLE     : begin
                            writeState <= WRITE_LOAD_UART;
                            ram_data   <= data;
                        end
                        WRITE_LOAD_UART  : begin
                            writeState <= WRITE_WRITE_UART;
                            uart_wrn   <= 1'b0;
                        end
                        WRITE_WRITE_UART : begin
                            writeState <= WRITE_WAIT_TBRE;
                            uart_wrn   <= 1'b1;
                        end
                        WRITE_WAIT_TBRE  : begin
                            if (uart_tbre)
                                writeState <= WRITE_WAIT_TSRE;
                        end
                        WRITE_WAIT_TSRE  : begin
                            if (uart_tsre)
                                writeState <= WRITE_IDLE;
                        end
                        default          : begin
                            writeState <= WRITE_IDLE;
                            uart_wrn   <= 1'b1;
                            ram_oe_n   <= 1'b1;
                        end
                    endcase
                end
                2'b00 : begin
                    // 跑飞了
                    mainState  <= MAIN_IDLE;
                    readState  <= READ_IDLE;
                    writeState <= WRITE_IDLE;
                end
            end
        end
    end



endmodule
