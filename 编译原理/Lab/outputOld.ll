; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
main_entry:
  %prime_counter = alloca i32, align 4
  store i32 0, i32* %prime_counter, align 4
  %current_num = alloca i32, align 4
  store i32 2, i32* %current_num, align 4
  %max_check = alloca i32, align 4
  store i32 50, i32* %max_check, align 4
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
  %fact_num = alloca i32, align 4
  store i32 5, i32* %fact_num, align 4
  %fact_counter = alloca i32, align 4
  store i32 1, i32* %fact_counter, align 4
  br label %while_cond_while_2142003995

while_cond_while_2142003995:                      ; preds = %if_next_55331187, %main_entry
  %current_num1 = load i32, i32* %current_num, align 4
  %max_check2 = load i32, i32* %max_check, align 4
  %lt_54495403tmp = icmp slt i32 %current_num1, %max_check2
  %lt_54495403_ext = zext i1 %lt_54495403tmp to i32
  %while_cond_while_21420039953 = icmp ne i32 %lt_54495403_ext, 0
  br i1 %while_cond_while_21420039953, label %while_body_while_2142003995, label %while_next_while_2142003995

while_body_while_2142003995:                      ; preds = %while_cond_while_2142003995
  %is_prime = alloca i32, align 4
  store i32 1, i32* %is_prime, align 4
  %divisor = alloca i32, align 4
  store i32 2, i32* %divisor, align 4
  br label %while_cond_while_1150538133

while_next_while_2142003995:                      ; preds = %while_cond_while_2142003995
  br label %while_cond_while_198761306

while_cond_while_1150538133:                      ; preds = %if_next_733957003, %while_body_while_2142003995
  %divisor4 = load i32, i32* %divisor, align 4
  %divisor5 = load i32, i32* %divisor, align 4
  %mul_op_662822946 = mul i32 %divisor4, %divisor5
  %current_num6 = load i32, i32* %current_num, align 4
  %le_1384722895tmp = icmp sle i32 %mul_op_662822946, %current_num6
  %le_1384722895_ext = zext i1 %le_1384722895tmp to i32
  %while_cond_while_11505381337 = icmp ne i32 %le_1384722895_ext, 0
  br i1 %while_cond_while_11505381337, label %while_body_while_1150538133, label %while_next_while_1150538133

while_body_while_1150538133:                      ; preds = %while_cond_while_1150538133
  %current_num8 = load i32, i32* %current_num, align 4
  %divisor9 = load i32, i32* %divisor, align 4
  %mod_op_245475541 = srem i32 %current_num8, %divisor9
  %eq_22429093tmp = icmp eq i32 %mod_op_245475541, 0
  %eq_22429093_ext = zext i1 %eq_22429093tmp to i32
  %if_cond_733957003 = icmp ne i32 %eq_22429093_ext, 0
  br i1 %if_cond_733957003, label %if_true_733957003, label %if_false_733957003

while_next_while_1150538133:                      ; preds = %if_true_733957003, %while_cond_while_1150538133
  %is_prime11 = load i32, i32* %is_prime, align 4
  %eq_868737467tmp = icmp eq i32 %is_prime11, 1
  %eq_868737467_ext = zext i1 %eq_868737467tmp to i32
  %if_cond_55331187 = icmp ne i32 %eq_868737467_ext, 0
  br i1 %if_cond_55331187, label %if_true_55331187, label %if_false_55331187

if_true_733957003:                                ; preds = %while_body_while_1150538133
  store i32 0, i32* %is_prime, align 4
  br label %while_next_while_1150538133

if_false_733957003:                               ; preds = %while_body_while_1150538133
  br label %if_next_733957003

if_next_733957003:                                ; preds = %if_false_733957003
  %divisor10 = load i32, i32* %divisor, align 4
  %add_op_815992954 = add i32 %divisor10, 1
  store i32 %add_op_815992954, i32* %divisor, align 4
  br label %while_cond_while_1150538133

if_true_55331187:                                 ; preds = %while_next_while_1150538133
  %total_primes12 = load i32, i32* %total_primes, align 4
  %current_num13 = load i32, i32* %current_num, align 4
  %add_op_1392425346 = add i32 %total_primes12, %current_num13
  store i32 %add_op_1392425346, i32* %total_primes, align 4
  %current_num14 = load i32, i32* %current_num, align 4
  %gt_2054574951tmp = icmp sgt i32 %current_num14, 10
  %gt_2054574951_ext = zext i1 %gt_2054574951tmp to i32
  %if_cond_1991294891 = icmp ne i32 %gt_2054574951_ext, 0
  br i1 %if_cond_1991294891, label %if_true_1991294891, label %if_false_1991294891

if_false_55331187:                                ; preds = %while_next_while_1150538133
  %prime_counter16 = load i32, i32* %prime_counter, align 4
  %sub_op_809762318 = sub i32 %prime_counter16, 1
  store i32 %sub_op_809762318, i32* %prime_counter, align 4
  br label %if_next_55331187

if_next_55331187:                                 ; preds = %if_false_55331187, %if_next_1991294891
  %current_num17 = load i32, i32* %current_num, align 4
  %add_op_2028371466 = add i32 %current_num17, 1
  store i32 %add_op_2028371466, i32* %current_num, align 4
  br label %while_cond_while_2142003995

if_true_1991294891:                               ; preds = %if_true_55331187
  %prime_counter15 = load i32, i32* %prime_counter, align 4
  %add_op_399931359 = add i32 %prime_counter15, 1
  store i32 %add_op_399931359, i32* %prime_counter, align 4
  br label %if_next_1991294891

if_false_1991294891:                              ; preds = %if_true_55331187
  br label %if_next_1991294891

if_next_1991294891:                               ; preds = %if_false_1991294891, %if_true_1991294891
  br label %if_next_55331187

while_cond_while_198761306:                       ; preds = %if_next_110771485, %while_next_while_2142003995
  %fib_count18 = load i32, i32* %fib_count, align 4
  %lt_798244209tmp = icmp slt i32 %fib_count18, 15
  %lt_798244209_ext = zext i1 %lt_798244209tmp to i32
  %while_cond_while_19876130619 = icmp ne i32 %lt_798244209_ext, 0
  br i1 %while_cond_while_19876130619, label %while_body_while_198761306, label %while_next_while_198761306

while_body_while_198761306:                       ; preds = %while_cond_while_198761306
  %fib_a20 = load i32, i32* %fib_a, align 4
  %mod_op_525571 = srem i32 %fib_a20, 2
  %eq_1263877414tmp = icmp eq i32 %mod_op_525571, 0
  %eq_1263877414_ext = zext i1 %eq_1263877414tmp to i32
  %if_cond_110771485 = icmp ne i32 %eq_1263877414_ext, 0
  br i1 %if_cond_110771485, label %if_true_110771485, label %if_false_110771485

while_next_while_198761306:                       ; preds = %while_cond_while_198761306
  br label %while_cond_while_1018298342

if_true_110771485:                                ; preds = %while_body_while_198761306
  %even_fib_sum21 = load i32, i32* %even_fib_sum, align 4
  %fib_a22 = load i32, i32* %fib_a, align 4
  %add_op_141289226 = add i32 %even_fib_sum21, %fib_a22
  store i32 %add_op_141289226, i32* %even_fib_sum, align 4
  br label %if_next_110771485

if_false_110771485:                               ; preds = %while_body_while_198761306
  %even_fib_sum23 = load i32, i32* %even_fib_sum, align 4
  %fib_a24 = load i32, i32* %fib_a, align 4
  %div_op_1208736537 = sdiv i32 %fib_a24, 2
  %sub_op_710239027 = sub i32 %even_fib_sum23, %div_op_1208736537
  store i32 %sub_op_710239027, i32* %even_fib_sum, align 4
  br label %if_next_110771485

if_next_110771485:                                ; preds = %if_false_110771485, %if_true_110771485
  %fib_a25 = load i32, i32* %fib_a, align 4
  %fib_b26 = load i32, i32* %fib_b, align 4
  %add_op_2104545713 = add i32 %fib_a25, %fib_b26
  store i32 %add_op_2104545713, i32* %fib_temp, align 4
  %fib_b27 = load i32, i32* %fib_b, align 4
  store i32 %fib_b27, i32* %fib_a, align 4
  %fib_temp28 = load i32, i32* %fib_temp, align 4
  store i32 %fib_temp28, i32* %fib_b, align 4
  %fib_count29 = load i32, i32* %fib_count, align 4
  %add_op_712256162 = add i32 %fib_count29, 1
  store i32 %add_op_712256162, i32* %fib_count, align 4
  br label %while_cond_while_198761306

while_cond_while_1018298342:                      ; preds = %if_next_561247961, %while_next_while_198761306
  %fact_counter30 = load i32, i32* %fact_counter, align 4
  %fact_num31 = load i32, i32* %fact_num, align 4
  %le_1039949752tmp = icmp sle i32 %fact_counter30, %fact_num31
  %le_1039949752_ext = zext i1 %le_1039949752tmp to i32
  %while_cond_while_101829834232 = icmp ne i32 %le_1039949752_ext, 0
  br i1 %while_cond_while_101829834232, label %while_body_while_1018298342, label %while_next_while_1018298342

while_body_while_1018298342:                      ; preds = %while_cond_while_1018298342
  %factorial33 = load i32, i32* %factorial, align 4
  %fact_counter34 = load i32, i32* %fact_counter, align 4
  %mul_op_1182461167 = mul i32 %factorial33, %fact_counter34
  store i32 %mul_op_1182461167, i32* %factorial, align 4
  %fact_counter35 = load i32, i32* %fact_counter, align 4
  %mod_op_1297149880 = srem i32 %fact_counter35, 2
  %eq_2116908859tmp = icmp eq i32 %mod_op_1297149880, 0
  %eq_2116908859_ext = zext i1 %eq_2116908859tmp to i32
  %if_cond_561247961 = icmp ne i32 %eq_2116908859_ext, 0
  br i1 %if_cond_561247961, label %if_true_561247961, label %if_false_561247961

while_next_while_1018298342:                      ; preds = %while_cond_while_1018298342
  %final_result = alloca i32, align 4
  store i32 0, i32* %final_result, align 4
  %total_primes38 = load i32, i32* %total_primes, align 4
  %even_fib_sum39 = load i32, i32* %even_fib_sum, align 4
  %gt_1863932867tmp = icmp sgt i32 %total_primes38, %even_fib_sum39
  %gt_1863932867_ext = zext i1 %gt_1863932867tmp to i32
  %if_cond_1373810119 = icmp ne i32 %gt_1863932867_ext, 0
  br i1 %if_cond_1373810119, label %if_true_1373810119, label %if_false_1373810119

if_true_561247961:                                ; preds = %while_body_while_1018298342
  %fact_counter36 = load i32, i32* %fact_counter, align 4
  %add_op_813656972 = add i32 %fact_counter36, 2
  store i32 %add_op_813656972, i32* %fact_counter, align 4
  br label %if_next_561247961

if_false_561247961:                               ; preds = %while_body_while_1018298342
  %fact_counter37 = load i32, i32* %fact_counter, align 4
  %add_op_2048425748 = add i32 %fact_counter37, 1
  store i32 %add_op_2048425748, i32* %fact_counter, align 4
  br label %if_next_561247961

if_next_561247961:                                ; preds = %if_false_561247961, %if_true_561247961
  br label %while_cond_while_1018298342

if_true_1373810119:                               ; preds = %while_next_while_1018298342
  %factorial40 = load i32, i32* %factorial, align 4
  %prime_counter41 = load i32, i32* %prime_counter, align 4
  %sub_op_445288316 = sub i32 %factorial40, %prime_counter41
  store i32 %sub_op_445288316, i32* %final_result, align 4
  br label %if_next_1373810119

if_false_1373810119:                              ; preds = %while_next_while_1018298342
  %total_primes42 = load i32, i32* %total_primes, align 4
  %even_fib_sum43 = load i32, i32* %even_fib_sum, align 4
  %lt_592688102tmp = icmp slt i32 %total_primes42, %even_fib_sum43
  %lt_592688102_ext = zext i1 %lt_592688102tmp to i32
  %if_cond_103887628 = icmp ne i32 %lt_592688102_ext, 0
  br i1 %if_cond_103887628, label %if_true_103887628, label %if_false_103887628

if_next_1373810119:                               ; preds = %if_next_103887628, %if_true_1373810119
  br label %while_cond_while_842326585

if_true_103887628:                                ; preds = %if_false_1373810119
  %factorial44 = load i32, i32* %factorial, align 4
  %prime_counter45 = load i32, i32* %prime_counter, align 4
  %add_op_1123629720 = add i32 %factorial44, %prime_counter45
  store i32 %add_op_1123629720, i32* %final_result, align 4
  br label %if_next_103887628

if_false_103887628:                               ; preds = %if_false_1373810119
  %prime_counter46 = load i32, i32* %prime_counter, align 4
  %mul_op_205962452 = mul i32 %prime_counter46, 2
  store i32 %mul_op_205962452, i32* %final_result, align 4
  br label %if_next_103887628

if_next_103887628:                                ; preds = %if_false_103887628, %if_true_103887628
  br label %if_next_1373810119

while_cond_while_842326585:                       ; preds = %if_next_438135304, %if_next_1373810119
  %final_result47 = load i32, i32* %final_result, align 4
  %gt_1032986144tmp = icmp sgt i32 %final_result47, 0
  %gt_1032986144_ext = zext i1 %gt_1032986144tmp to i32
  %while_cond_while_84232658548 = icmp ne i32 %gt_1032986144_ext, 0
  br i1 %while_cond_while_84232658548, label %while_body_while_842326585, label %while_next_while_842326585

while_body_while_842326585:                       ; preds = %while_cond_while_842326585
  %final_result49 = load i32, i32* %final_result, align 4
  %mod_op_917819120 = srem i32 %final_result49, 3
  %eq_263025902tmp = icmp eq i32 %mod_op_917819120, 0
  %eq_263025902_ext = zext i1 %eq_263025902tmp to i32
  %if_cond_438135304 = icmp ne i32 %eq_263025902_ext, 0
  br i1 %if_cond_438135304, label %if_true_438135304, label %if_false_438135304

while_next_while_842326585:                       ; preds = %while_cond_while_842326585
  %final_result54 = load i32, i32* %final_result, align 4
  ret i32 %final_result54

if_true_438135304:                                ; preds = %while_body_while_842326585
  %final_result50 = load i32, i32* %final_result, align 4
  %div_op_936580213 = sdiv i32 %final_result50, 2
  store i32 %div_op_936580213, i32* %final_result, align 4
  br label %if_next_438135304

if_false_438135304:                               ; preds = %while_body_while_842326585
  %final_result51 = load i32, i32* %final_result, align 4
  %mod_op_662736689 = srem i32 %final_result51, 4
  %eq_1131316523tmp = icmp eq i32 %mod_op_662736689, 1
  %eq_1131316523_ext = zext i1 %eq_1131316523tmp to i32
  %if_cond_852687460 = icmp ne i32 %eq_1131316523_ext, 0
  br i1 %if_cond_852687460, label %if_true_852687460, label %if_false_852687460

if_next_438135304:                                ; preds = %if_next_852687460, %if_true_438135304
  br label %while_cond_while_842326585

if_true_852687460:                                ; preds = %if_false_438135304
  %final_result52 = load i32, i32* %final_result, align 4
  %sub_op_495792375 = sub i32 %final_result52, 5
  store i32 %sub_op_495792375, i32* %final_result, align 4
  br label %if_next_852687460

if_false_852687460:                               ; preds = %if_false_438135304
  %final_result53 = load i32, i32* %final_result, align 4
  %sub_op_1045941616 = sub i32 %final_result53, 1
  store i32 %sub_op_1045941616, i32* %final_result, align 4
  br label %if_next_852687460

if_next_852687460:                                ; preds = %if_false_852687460, %if_true_852687460
  br label %if_next_438135304
}
