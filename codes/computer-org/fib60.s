    .text
    .global fibb
fibb:
    addi    sp, sp, -4
    sw      ra, 0(sp)
    
    li      t1, 0x80400000 # t1 = 0x80400000

    li      a0, 0
    li      a1, 0

    li      a2, 1
    li      a3, 0

    li      t5, 2
    li      t6, 60
    
    .loop:
    bge     t5, t6, .done # if t5 >= t6 then .done

    add     a4, a0, a2
    sltu    t2, a4, a0
    add     a5, a1, a3
    add     a5, a5, t2


    addi    t5, t5, 1

    mv      a1, a3
    mv      a0, a2
    mv      a3, a5
    mv      a2, a4

    j       .loop

    .done:
    sw      a4, 0(t1)
    sw      a5, 4(t1)

    lw      ra, 0(sp) # 
    addi    sp, sp, 4 # sp = sp + framesize
    ret
