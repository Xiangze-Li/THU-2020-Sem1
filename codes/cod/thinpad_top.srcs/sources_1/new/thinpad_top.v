`default_nettype none

module thinpad_top
(
    input wire clk_50M,           //50MHz 时钟输入
    input wire clk_11M0592,       //11.0592MHz 时钟输入（备用，可不用）

    input wire clock_btn,         //BTN5手动时钟按钮开关，带消抖电路，按下时为1
    input wire reset_btn,         //BTN6手动复位按钮开关，带消抖电路，按下时为1

    input  wire[3:0]  touch_btn,  //BTN1~BTN4，按钮开关，按下时为1
    input  wire[31:0] dip_sw,     //32位拨码开关，拨到"ON"时为1
    output wire[15:0] leds,       //16位LED，输出时1点亮
    output wire[7:0]  dpy0,       //数码管低位信号，包括小数点，输出1点亮
    output wire[7:0]  dpy1,       //数码管高位信号，包括小数点，输出1点亮

    //CPLD串口控制器信号
    output wire uart_rdn,         //读串口信号，低有效
    output wire uart_wrn,         //写串口信号，低有效
    input wire uart_dataready,    //串口数据准备好
    input wire uart_tbre,         //发送数据标志
    input wire uart_tsre,         //数据发送完毕标志

    //BaseRAM信号
    inout wire[31:0] base_ram_data,  //BaseRAM数据，低8位与CPLD串口控制器共享
    output wire[19:0] base_ram_addr, //BaseRAM地址
    output wire[3:0] base_ram_be_n,  //BaseRAM字节使能，低有效。如果不使用字节使能，请保持为0
    output wire base_ram_ce_n,       //BaseRAM片选，低有效
    output wire base_ram_oe_n,       //BaseRAM读使能，低有效
    output wire base_ram_we_n,       //BaseRAM写使能，低有效

    //ExtRAM信号
    inout wire[31:0] ext_ram_data,  //ExtRAM数据
    output wire[19:0] ext_ram_addr, //ExtRAM地址
    output wire[3:0] ext_ram_be_n,  //ExtRAM字节使能，低有效。如果不使用字节使能，请保持为0
    output wire ext_ram_ce_n,       //ExtRAM片选，低有效
    output wire ext_ram_oe_n,       //ExtRAM读使能，低有效
    output wire ext_ram_we_n,       //ExtRAM写使能，低有效

    //直连串口信号
    output wire txd,  //直连串口发送端
    input  wire rxd,  //直连串口接收端

    //Flash存储器信号，参考 JS28F640 芯片手册
    output wire [22:0]flash_a,      //Flash地址，a0仅在8bit模式有效，16bit模式无意义
    inout  wire [15:0]flash_d,      //Flash数据
    output wire flash_rp_n,         //Flash复位信号，低有效
    output wire flash_vpen,         //Flash写保护信号，低电平时不能擦除、烧写
    output wire flash_ce_n,         //Flash片选信号，低有效
    output wire flash_oe_n,         //Flash读使能信号，低有效
    output wire flash_we_n,         //Flash写使能信号，低有效
    output wire flash_byte_n,       //Flash 8bit模式选择，低有效。在使用flash的16位模式时请设为1

    //USB 控制器信号，参考 SL811 芯片手册
    output wire sl811_a0,
    //inout  wire[7:0] sl811_d,     //USB数据线与网络控制器的dm9k_sd[7:0]共享
    output wire sl811_wr_n,
    output wire sl811_rd_n,
    output wire sl811_cs_n,
    output wire sl811_rst_n,
    output wire sl811_dack_n,
    input  wire sl811_intrq,
    input  wire sl811_drq_n,

    //网络控制器信号，参考 DM9000A 芯片手册
    output wire dm9k_cmd,
    inout  wire[15:0] dm9k_sd,
    output wire dm9k_iow_n,
    output wire dm9k_ior_n,
    output wire dm9k_cs_n,
    output wire dm9k_pwrst_n,
    input  wire dm9k_int,

    //图像输出信号
    output wire[2:0] video_red,    //红色像素，3位
    output wire[2:0] video_green,  //绿色像素，3位
    output wire[1:0] video_blue,   //蓝色像素，2位
    output wire video_hsync,       //行同步（水平同步）信号
    output wire video_vsync,       //场同步（垂直同步）信号
    output wire video_clk,         //像素时钟输出
    output wire video_de           //行数据有效信号，用于区分消隐区
);

    wire clk_10M, clk_25M, rst_10M, rst_25M;
    ClkGen ( clk_50M, reset_btn, clk_10M, clk_25M, rst_10M, rst_25M );

    parameter [2:0]
    // Stages
        STDBY = 3'b000,
        IF = 3'b001,
        ID = 3'b010,
        EX = 3'b011,
        ME = 3'b100,
        WB = 3'b101,
        ERR = 3'b111;
    reg  [2:0]  stage;
    wire [2:0]  stageNext;

    reg  [31:0] pc, pcNow;

    wire        regWr, flagZ,
                pcWr, pcNowWr, pcSel,
                memSel, memWr, memRd,
                irWr;
    wire [2:0]  immSel, aluASel, aluBSel, regDSel,
                aluOp, aluAlter;
    wire [4:0]  rs1, rs2, rd;
    wire [31:0] immOut,
                oprandA, oprandB,
                data2RF, q1, q2, aluResult, dataFrRam,
                pcSrc,
                baseA;
    reg  [31:0] rA, rB, rC,
                rI, rD;

    assign rs1 = rI[19:15];
    assign rs2 = rI[24:20];
    assign rd  = rI[11:07];
    assign pcSrc = pcSel ? aluResult : rC;
    assign baseA = memSel ? rC : pc;
    assign data2RF = regDSel == 2'b00 ? rD :
                     regDSel == 2'b01 ? rC :
                     regDSel == 2'b10 ? pc :
            /* regDSel==3 UNDEFINED */  32'b0;
    assign oprandA = aluASel == 2'b00 ? pc :
                     aluASel == 2'b01 ? pcNow :
                     aluASel == 2'b10 ? rA :
                     /* 3 UNDEFINED */  32'b0;
    assign oprandB = aluBSel == 2'b00 ? 32'h4 :
                     aluBSel == 2'b01 ? rB :
                     aluBSel == 2'b10 ? immOut :
                     /* 3 UNDEFINED */  32'b0;

    RegFile rf(
        .clk(clk_10M),
        .regWr(regWr),
        .rs1(rs1),
        .rs2(rs2),
        .rd(rd),
        .data(data2RF),

        .q1(q1),
        .q2(q2)
    );

    ImmGen imm(
        .inst(rI),
        .immSel(immSel),

        .immOut(immOut)
    );

    Decoder dec(
        .inst(rI),
        .flagZ(flagZ),
        .stage(stage),

        .pcWr(pcWr),
        .pcNowWr(pcNowWr),
        .pcSel(pcSel),
        .memSel(memSel),
        .memWr(memWr),
        .memRd(memRd),
        .irWr(irWr),
        .regDSel(regDSel),
        .immSel(immSel),
        .regWr(regWr),
        .aluASel(aluASel),
        .aluBSel(aluBSel),
        .aluOp(aluOp),
        .aluAlter(aluAlter),

        .stageNext(stageNext)
    );

    ALU alu(
        .opCode(aluOp),
        .alter(aluAlter),
        .oprandA(oprandA),
        .oprandB(oprandB),

        .result(aluResult),
        .flagZero(flagZ)
    );

    MemController memctrl(
        .clk(clk_10M),

        .baseDIn(rB),
        .baseDOut(dataFrRam),
        .baseA(baseA),
        .baseWr(memWr),
        .baseRd(memRd),

        .baseData(base_ram_data),
        .baseAddr(base_ram_addr),
        .baseCeN(base_ram_ce_n),
        .baseBeN(base_ram_be_n),
        .baseOeN(base_ram_oe_n),
        .baseWeN(base_ram_we_n),

        .extData(ext_ram_data),
        .extAddr(ext_ram_addr),
        .extCeN(ext_ram_ce_n),
        .extBeN(ext_ram_be_n),
        .extOeN(ext_ram_oe_n),
        .extWeN(ext_ram_we_n),

        .uartDataready(uart_dataready),
        .uartTbrE(uart_tbre),
        .uartTsrE(uart_tsre),
        .uartRdN(uart_rdn),
        .uartWrN(uart_wrn)
    );

    always @(posedge clk_10M) begin
        rA <= q1;
        rB <= q2;
        rC <= aluResult;
        rD <= dataFrRam;

        stage <= stageNext;

        if (pcWr)
            pc <= pcSrc;

        if (pcNowWr)
            pcNow <= pc;

        if (irWr)
            rI <= dataFrRam;
    end

endmodule
