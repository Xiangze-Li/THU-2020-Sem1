    .text
    .global fibb
fibb:
    addi    sp, sp, -4
    sw      ra, 4-4(sp)
    
    li      t1, 0x80400000 # t1 = 0x80400000

    li      t2, 0
    sw      t2, 0(t1) # 
    addi    t1, t1, 4

    li      t3, 1
    sw      t3, 0(t1)
    addi    t1, t1, 4

    li      t5, 2
    li      t6, 10
    
    .loop:
    bge     t5, t6, .done # if t5 >= t6 then .done
    add     t4, t2, t3 # t4 = t2 + t3
    sw      t4, 0(t1) # 
    addi    t1, t1, 4
    addi    t5, t5, 1
    mv      t2, t3
    mv      t3, t4
    j       .loop

    .done:
    lw      ra, 4-4(sp) # 
    addi    sp, sp, 4 # sp = sp + framesize
    ret
