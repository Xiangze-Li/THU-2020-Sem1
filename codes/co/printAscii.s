    .text
    .global print_ascii
print_ascii:
    addi    sp, sp, -12
    sw      ra, 8(sp)
    sw      s1, 4(sp)
    sw      s2, 0(sp)

    li      s1, 0x21
    li      s2, 0x7F

    .loop:
    bgt     s1, s2, .done
    mv      a0, s1
    # li      s0, 30
    # ecall
    jal     WRITE_SERIAL

    addi    s1, s1, 1
    j       .loop

    .done:
    lw      s2, 0(sp)
    lw      s1, 4(sp)
    lw      ra, 8(sp)
    addi    sp, sp, 12
    jr      ra

WRITE_SERIAL:                       
    li t0, 0x10000000
.TESTW:
    lb t1, %lo(5)(t0)  
    andi t1, t1, 0x20       
    bne t1, zero, .WSERIAL         
    j .TESTW                     
.WSERIAL:
    sb a0, %lo(0)(t0)  
    jr ra

