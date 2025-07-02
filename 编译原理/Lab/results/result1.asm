  .text
  .globl main
main:
  addi sp, sp, -80      # prologue
main_entry:
  li t0, 0
  mv s0, t0
  li t0, 5
  mv s1, t0
  li t0, 0
  mv s2, t0
  j while_cond_while_1698097425
while_cond_while_1698097425:
  mv s3, s0
  mv s4, s1
  mv t0, s3
  mv t1, s4
  slt t0, t0, t1
  mv s5, t0
  mv t0, s5
  mv s6, t0
  mv t0, s6
  li t1, 0
  xor t0, t0, t1
  snez t0, t0
  mv s7, t0
  mv t0, s7
  bne t0, x0, while_body_while_1698097425
  j while_next_while_1698097425
while_body_while_1698097425:
  li t0, 10
  mv s8, t0
  li t0, 10
  mv s9, t0
  li t0, 10
  mv s10, t0
  li t0, 10
  mv s11, t0
  li t0, 10
  mv a0, t0
  li t0, 10
  mv a1, t0
  li t0, 10
  mv a2, t0
  li t0, 10
  mv a3, t0
  li t0, 10
  mv a4, t0
  li t0, 10
  mv a5, t0
  li t0, 10
  mv a6, t0
  li t0, 10
  mv a7, t0
  li t0, 10
  mv t2, t0
  li t0, 10
  mv t3, t0
  li t0, 10
  mv t4, t0
  li t0, 10
  mv t5, t0
  li t0, 10
  mv t6, t0
  li t0, 10
  mv a6, t0
  li t0, 10
  sw t0, 76(sp)
  li t0, 10
  sw t0, 72(sp)
  li t0, 10
  sw t0, 68(sp)
  li t0, 10
  sw t0, 64(sp)
  li t0, 10
  sw t0, 60(sp)
  li t0, 10
  sw t0, 56(sp)
  li t0, 10
  sw t0, 52(sp)
  li t0, 10
  sw t0, 48(sp)
  li t0, 10
  sw t0, 44(sp)
  li t0, 10
  sw t0, 40(sp)
  li t0, 10
  sw t0, 36(sp)
  li t0, 10
  sw t0, 32(sp)
  li t0, 10
  sw t0, 28(sp)
  li t0, 10
  sw t0, 24(sp)
  sw s2, 20(sp)
  sw s8, 16(sp)
  lw t0, 20(sp)
  lw t1, 16(sp)
  add t0, t0, t1
  sw t0, 12(sp)
  mv s8, s9
  lw t0, 12(sp)
  mv t1, s8
  add t0, t0, t1
  sw t0, 8(sp)
  mv s9, s10
  lw t0, 8(sp)
  mv t1, s9
  add t0, t0, t1
  mv s8, t0
  mv s10, s11
  mv t0, s8
  mv t1, s10
  add t0, t0, t1
  mv s9, t0
  mv s11, a0
  mv t0, s9
  mv t1, s11
  add t0, t0, t1
  mv s8, t0
  mv s10, a1
  mv t0, s8
  mv t1, s10
  add t0, t0, t1
  mv a0, t0
  mv s9, a2
  mv t0, a0
  mv t1, s9
  add t0, t0, t1
  mv s11, t0
  mv a1, a3
  mv t0, s11
  mv t1, a1
  add t0, t0, t1
  mv s8, t0
  mv s10, a4
  mv t0, s8
  mv t1, s10
  add t0, t0, t1
  mv a2, t0
  mv t0, a2
  mv s2, t0
  mv a0, s2
  mv s9, a5
  mv t0, a0
  mv t1, s9
  add t0, t0, t1
  mv a3, t0
  mv s11, a7
  mv t0, a3
  mv t1, s11
  add t0, t0, t1
  mv a1, t0
  mv a4, t2
  mv t0, a1
  mv t1, a4
  add t0, t0, t1
  mv s8, t0
  mv s10, t3
  mv t0, s8
  mv t1, s10
  add t0, t0, t1
  mv a2, t0
  mv a5, t4
  mv t0, a2
  mv t1, a5
  add t0, t0, t1
  mv a0, t0
  mv s9, t5
  mv t0, a0
  mv t1, s9
  add t0, t0, t1
  mv a7, t0
  mv a3, t6
  mv t0, a7
  mv t1, a3
  add t0, t0, t1
  mv s11, t0
  mv t2, a6
  mv t0, s11
  mv t1, t2
  add t0, t0, t1
  mv a1, t0
  lw t0, 76(sp)
  mv a4, t0
  mv t0, a1
  mv t1, a4
  add t0, t0, t1
  mv t3, t0
  mv t0, t3
  mv s2, t0
  mv s8, s2
  lw t0, 72(sp)
  mv s10, t0
  mv t0, s8
  mv t1, s10
  add t0, t0, t1
  mv t4, t0
  lw t0, 64(sp)
  mv a2, t0
  mv t0, t4
  mv t1, a2
  add t0, t0, t1
  mv a5, t0
  lw t0, 60(sp)
  mv t5, t0
  mv t0, a5
  mv t1, t5
  add t0, t0, t1
  mv a0, t0
  lw t0, 56(sp)
  mv s9, t0
  mv t0, a0
  mv t1, s9
  add t0, t0, t1
  mv t6, t0
  lw t0, 52(sp)
  mv a7, t0
  mv t0, t6
  mv t1, a7
  add t0, t0, t1
  mv a3, t0
  lw t0, 48(sp)
  mv a6, t0
  mv t0, a3
  mv t1, a6
  add t0, t0, t1
  mv s11, t0
  lw t0, 44(sp)
  mv t2, t0
  mv t0, s11
  mv t1, t2
  add t0, t0, t1
  mv a1, t0
  lw t0, 40(sp)
  mv a4, t0
  mv t0, a1
  mv t1, a4
  add t0, t0, t1
  mv t3, t0
  lw t0, 36(sp)
  mv s8, t0
  mv t0, t3
  mv t1, s8
  add t0, t0, t1
  mv s10, t0
  mv t0, s10
  mv s2, t0
  mv t4, s2
  lw t0, 32(sp)
  mv a2, t0
  mv t0, t4
  mv t1, a2
  add t0, t0, t1
  mv a5, t0
  lw t0, 28(sp)
  mv t5, t0
  mv t0, a5
  mv t1, t5
  add t0, t0, t1
  mv a0, t0
  lw t0, 24(sp)
  mv s9, t0
  mv t0, a0
  mv t1, s9
  add t0, t0, t1
  mv t6, t0
  mv t0, t6
  mv s2, t0
  mv a7, s0
  mv t0, a7
  li t1, 1
  add t0, t0, t1
  mv a3, t0
  mv t0, a3
  mv s0, t0
  j while_cond_while_1698097425
while_next_while_1698097425:
  mv a6, s2
  mv a0, a6
  li a7, 93
  ecall
  li a0, 0
  addi sp, sp, 80       # epilogue
  li a7, 93
  ecall
