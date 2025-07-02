; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %prime_counter = alloca i32, align 4
  store i32 0, i32* %prime_counter, align 4
  %current_num = alloca i32, align 4
  store i32 2, i32* %current_num, align 4
  %total_primes = alloca i32, align 4
  store i32 0, i32* %total_primes, align 4
  %fib_a = alloca i32, align 4
  store i32 0, i32* %fib_a, align 4
  %fib_b = alloca i32, align 4
  store i32 1, i32* %fib_b, align 4
  %fib_temp = alloca i32, align 4
  store i32 0, i32* %fib_temp, align 4
  %fib_count = alloca i32, align 4
  store i32 1, i32* %fib_count, align 4
  %even_fib_sum = alloca i32, align 4
  store i32 0, i32* %even_fib_sum, align 4
  %factorial = alloca i32, align 4
  store i32 1, i32* %factorial, align 4
  %fact_counter = alloca i32, align 4
  store i32 1, i32* %fact_counter, align 4
  br label %WhileCond

WhileCond:                                        ; preds = %IfMerge14, %mainEntry
  %AccessLoad = load i32, i32* %current_num, align 4
  %LT = icmp slt i32 %AccessLoad, 50
  br i1 %LT, label %WhileLoop, label %WhileCond26

WhileLoop:                                        ; preds = %WhileCond
  %is_prime = alloca i32, align 4
  store i32 1, i32* %is_prime, align 4
  %divisor = alloca i32, align 4
  store i32 2, i32* %divisor, align 4
  br label %WhileCond2

WhileCond2:                                       ; preds = %IfMerge, %WhileLoop
  %AccessLoad3 = load i32, i32* %divisor, align 4
  %AccessLoad4 = load i32, i32* %divisor, align 4
  %Mul = mul i32 %AccessLoad3, %AccessLoad4
  %AccessLoad5 = load i32, i32* %current_num, align 4
  %LE = icmp sle i32 %Mul, %AccessLoad5
  br i1 %LE, label %WhileLoop6, label %WhileMerge7

WhileLoop6:                                       ; preds = %WhileCond2
  %AccessLoad8 = load i32, i32* %current_num, align 4
  %AccessLoad9 = load i32, i32* %divisor, align 4
  %Mod = srem i32 %AccessLoad8, %AccessLoad9
  %Eq = icmp eq i32 %Mod, 0
  br i1 %Eq, label %IfTrue, label %IfMerge

WhileMerge7:                                      ; preds = %IfTrue, %WhileCond2
  %AccessLoad11 = load i32, i32* %is_prime, align 4
  %Eq12 = icmp eq i32 %AccessLoad11, 1
  br i1 %Eq12, label %IfTrue13, label %IfFalse

IfTrue:                                           ; preds = %WhileLoop6
  store i32 0, i32* %is_prime, align 4
  br label %WhileMerge7

IfMerge:                                          ; preds = %WhileLoop6
  %AccessLoad10 = load i32, i32* %divisor, align 4
  %Add = add i32 %AccessLoad10, 1
  store i32 %Add, i32* %divisor, align 4
  br label %WhileCond2

IfTrue13:                                         ; preds = %WhileMerge7
  %AccessLoad15 = load i32, i32* %total_primes, align 4
  %AccessLoad16 = load i32, i32* %current_num, align 4
  %Add17 = add i32 %AccessLoad15, %AccessLoad16
  store i32 %Add17, i32* %total_primes, align 4
  %AccessLoad18 = load i32, i32* %current_num, align 4
  %GT = icmp sgt i32 %AccessLoad18, 10
  br i1 %GT, label %IfTrue19, label %IfMerge14

IfMerge14:                                        ; preds = %IfTrue19, %IfTrue13, %IfFalse, %IfMerge20
  %AccessLoad24 = load i32, i32* %current_num, align 4
  %Add25 = add i32 %AccessLoad24, 1
  store i32 %Add25, i32* %current_num, align 4
  br label %WhileCond

IfTrue19:                                         ; preds = %IfTrue13
  %AccessLoad21 = load i32, i32* %prime_counter, align 4
  %Add22 = add i32 %AccessLoad21, 1
  store i32 %Add22, i32* %prime_counter, align 4
  br label %IfMerge14

IfFalse:                                          ; preds = %WhileMerge7
  %AccessLoad23 = load i32, i32* %prime_counter, align 4
  %Sub = sub i32 %AccessLoad23, 1
  store i32 %Sub, i32* %prime_counter, align 4
  br label %IfMerge14

WhileCond26:                                      ; preds = %WhileCond, %IfMerge35, %WhileMerge
  %AccessLoad27 = load i32, i32* %fib_count, align 4
  %LT28 = icmp slt i32 %AccessLoad27, 15
  br i1 %LT28, label %WhileLoop29, label %WhileCond50

WhileLoop29:                                      ; preds = %WhileCond26
  %AccessLoad31 = load i32, i32* %fib_a, align 4
  %Mod32 = srem i32 %AccessLoad31, 2
  %Eq33 = icmp eq i32 %Mod32, 0
  br i1 %Eq33, label %IfTrue34, label %IfFalse39

IfTrue34:                                         ; preds = %WhileLoop29
  %AccessLoad36 = load i32, i32* %even_fib_sum, align 4
  %AccessLoad37 = load i32, i32* %fib_a, align 4
  %Add38 = add i32 %AccessLoad36, %AccessLoad37
  store i32 %Add38, i32* %even_fib_sum, align 4
  br label %IfMerge35

IfMerge35:                                        ; preds = %IfFalse39, %IfTrue34
  %AccessLoad43 = load i32, i32* %fib_a, align 4
  %AccessLoad44 = load i32, i32* %fib_b, align 4
  %Add45 = add i32 %AccessLoad43, %AccessLoad44
  store i32 %Add45, i32* %fib_temp, align 4
  %AccessLoad46 = load i32, i32* %fib_b, align 4
  store i32 %AccessLoad46, i32* %fib_a, align 4
  %AccessLoad47 = load i32, i32* %fib_temp, align 4
  store i32 %AccessLoad47, i32* %fib_b, align 4
  %AccessLoad48 = load i32, i32* %fib_count, align 4
  %Add49 = add i32 %AccessLoad48, 1
  store i32 %Add49, i32* %fib_count, align 4
  br label %WhileCond26

IfFalse39:                                        ; preds = %WhileLoop29
  %AccessLoad40 = load i32, i32* %even_fib_sum, align 4
  %AccessLoad41 = load i32, i32* %fib_a, align 4
  %Div = sdiv i32 %AccessLoad41, 2
  %Sub42 = sub i32 %AccessLoad40, %Div
  store i32 %Sub42, i32* %even_fib_sum, align 4
  br label %IfMerge35

WhileCond50:                                      ; preds = %IfTrue62, %IfFalse66, %WhileCond26, %IfMerge63, %WhileMerge30
  %AccessLoad51 = load i32, i32* %fact_counter, align 4
  %LE53 = icmp sle i32 %AccessLoad51, 5
  br i1 %LE53, label %WhileLoop54, label %WhileMerge55

WhileLoop54:                                      ; preds = %WhileCond50
  %AccessLoad56 = load i32, i32* %factorial, align 4
  %AccessLoad57 = load i32, i32* %fact_counter, align 4
  %Mul58 = mul i32 %AccessLoad56, %AccessLoad57
  store i32 %Mul58, i32* %factorial, align 4
  %AccessLoad59 = load i32, i32* %fact_counter, align 4
  %Mod60 = srem i32 %AccessLoad59, 2
  %Eq61 = icmp eq i32 %Mod60, 0
  br i1 %Eq61, label %IfTrue62, label %IfFalse66

WhileMerge55:                                     ; preds = %WhileCond50
  %final_result = alloca i32, align 4
  store i32 0, i32* %final_result, align 4
  %AccessLoad69 = load i32, i32* %total_primes, align 4
  %AccessLoad70 = load i32, i32* %even_fib_sum, align 4
  %GT71 = icmp sgt i32 %AccessLoad69, %AccessLoad70
  br i1 %GT71, label %IfTrue72, label %IfFalse77

IfTrue62:                                         ; preds = %WhileLoop54
  %AccessLoad64 = load i32, i32* %fact_counter, align 4
  %Add65 = add i32 %AccessLoad64, 2
  store i32 %Add65, i32* %fact_counter, align 4
  br label %WhileCond50

IfFalse66:                                        ; preds = %WhileLoop54
  %AccessLoad67 = load i32, i32* %fact_counter, align 4
  %Add68 = add i32 %AccessLoad67, 1
  store i32 %Add68, i32* %fact_counter, align 4
  br label %WhileCond50

IfTrue72:                                         ; preds = %WhileMerge55
  %AccessLoad74 = load i32, i32* %factorial, align 4
  %AccessLoad75 = load i32, i32* %prime_counter, align 4
  %Sub76 = sub i32 %AccessLoad74, %AccessLoad75
  store i32 %Sub76, i32* %final_result, align 4
  br label %WhileCond89

IfFalse77:                                        ; preds = %WhileMerge55
  %AccessLoad78 = load i32, i32* %total_primes, align 4
  %AccessLoad79 = load i32, i32* %even_fib_sum, align 4
  %LT80 = icmp slt i32 %AccessLoad78, %AccessLoad79
  br i1 %LT80, label %IfTrue81, label %IfFalse86

IfTrue81:                                         ; preds = %IfFalse77
  %AccessLoad83 = load i32, i32* %factorial, align 4
  %AccessLoad84 = load i32, i32* %prime_counter, align 4
  %Add85 = add i32 %AccessLoad83, %AccessLoad84
  store i32 %Add85, i32* %final_result, align 4
  br label %WhileCond89

IfFalse86:                                        ; preds = %IfFalse77
  %AccessLoad87 = load i32, i32* %prime_counter, align 4
  %Mul88 = mul i32 %AccessLoad87, 2
  store i32 %Mul88, i32* %final_result, align 4
  br label %WhileCond89

WhileCond89:                                      ; preds = %IfTrue81, %IfFalse86, %IfTrue105, %IfFalse109, %IfTrue97, %IfMerge106, %IfTrue72, %IfMerge82, %IfMerge98, %IfMerge73
  %AccessLoad90 = load i32, i32* %final_result, align 4
  %GT91 = icmp sgt i32 %AccessLoad90, 0
  br i1 %GT91, label %WhileLoop92, label %WhileMerge93

WhileLoop92:                                      ; preds = %WhileCond89
  %AccessLoad94 = load i32, i32* %final_result, align 4
  %Mod95 = srem i32 %AccessLoad94, 3
  %Eq96 = icmp eq i32 %Mod95, 0
  br i1 %Eq96, label %IfTrue97, label %IfFalse101

WhileMerge93:                                     ; preds = %WhileCond89
  %AccessLoad112 = load i32, i32* %final_result, align 4
  ret i32 %AccessLoad112

IfTrue97:                                         ; preds = %WhileLoop92
  %AccessLoad99 = load i32, i32* %final_result, align 4
  %Div100 = sdiv i32 %AccessLoad99, 2
  store i32 %Div100, i32* %final_result, align 4
  br label %WhileCond89

IfFalse101:                                       ; preds = %WhileLoop92
  %AccessLoad102 = load i32, i32* %final_result, align 4
  %Mod103 = srem i32 %AccessLoad102, 4
  %Eq104 = icmp eq i32 %Mod103, 1
  br i1 %Eq104, label %IfTrue105, label %IfFalse109

IfTrue105:                                        ; preds = %IfFalse101
  %AccessLoad107 = load i32, i32* %final_result, align 4
  %Sub108 = sub i32 %AccessLoad107, 5
  store i32 %Sub108, i32* %final_result, align 4
  br label %WhileCond89

IfFalse109:                                       ; preds = %IfFalse101
  %AccessLoad110 = load i32, i32* %final_result, align 4
  %Sub111 = sub i32 %AccessLoad110, 1
  store i32 %Sub111, i32* %final_result, align 4
  br label %WhileCond89
}
