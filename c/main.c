#pragma clang diagnostic push
#pragma ide diagnostic ignored "cert-err34-c"
#pragma ide diagnostic ignored "cert-msc50-cpp"
#pragma ide diagnostic ignored "cert-msc51-cpp"

#include <stdio.h>
#include <time.h>
#include <immintrin.h>
#include <stdlib.h>
#include <assert.h>


int data[] = {36, 31, 68, 73, 91, 33, 4, 30, 94, 28, 82, 46, 87, 89, 47, 63, 46, 97, 74, 60, 61, 24, 11,
              72,
              24,
              17, 22, 9, 65, 81, 71, 17, 88, 1, 20, 70, 94, 52, 94, 22, 95, 84, 93, 65, 10, 50, 99, 73,
              35,
              53,
              93, 46, 33, 85, 81, 52, 22, 91, 87, 70, 60, 94, 80, 59, 12, 44, 43, 68, 49, 33, 21, 40,
              51, 95,
              81,
              84, 18, 2, 94, 34, 98, 95, 44, 10, 4, 35, 97, 57, 2, 98, 58, 4, 52, 15, 40, 20, 84, 48,
              60, 21,
              71,
              47, 53, 38, 95, 69, 58, 32, 66, 37, 72, 29, 32, 63, 1, 94, 8, 67, 88, 77, 51, 77, 71, 83,
              62,
              33,
              32, 89, 5, 17, 81, 37, 9, 32, 9, 7, 83, 28, 60, 14, 9, 86, 43, 5, 82, 68, 82, 99, 98, 96,
              38,
              54,
              97, 51, 34, 90, 47, 89, 12, 9, 16, 39, 41, 66, 99, 27, 78, 38, 50, 50, 67, 39, 70, 78, 58,
              21,
              47,
              11, 46, 41, 21, 96, 24, 44, 80, 95, 56, 13, 94, 79, 43, 48, 86, 64, 9, 24, 31, 49, 30, 58,
              43,
              38,
              31, 39, 65, 84, 30, 3, 3, 20, 73, 63, 84, 76, 14, 24, 77, 51, 50, 22, 7, 11, 58, 40, 72,
              35, 76,
              20,
              44, 94, 6, 3, 95, 50, 32, 4, 81, 71, 67, 45, 69, 12, 21, 78, 49, 96, 50, 3, 47, 53, 35,
              47, 75,
              40,
              24, 34, 68, 38, 48, 2, 69, 22, 19, 24, 27, 27, 70, 41, 62, 55, 91, 3, 72, 16, 52, 40, 55,
              68,
              79,
              13, 71, 27, 64, 24, 63, 70, 60, 48, 36, 83, 16, 86, 99, 69, 15, 5, 24, 30, 63, 98, 89, 51,
              9,
              40,
              57, 67, 96, 32, 42, 60, 79, 86, 57, 48, 65, 56, 69, 98, 43, 21, 92, 20, 38, 40, 24, 62,
              62, 5,
              54,
              50, 58, 53, 32, 31, 17, 11, 20, 30, 95, 53, 21, 47, 86, 51, 11, 69, 95, 48, 56, 91, 49,
              29, 12,
              95,
              19, 55, 79, 42, 50, 12, 49, 20, 57, 20, 11, 29, 71, 64, 68, 86, 50, 65, 60, 90, 93, 75,
              65, 4,
              6,
              67, 95, 34, 42, 54, 13, 58, 52, 5, 11, 79, 37, 41, 83, 9, 96, 43, 15, 32, 30, 35, 29, 98,
              53,
              80,
              74, 89, 65, 30, 14, 69, 73, 82, 87, 61, 50, 44, 53, 12, 39, 37, 47, 21, 82, 66, 8, 49, 99,
              52,
              97,
              66, 51, 33, 35, 19, 45, 28, 86, 90, 20, 43, 24, 37, 39, 83, 27, 2, 12, 4, 77, 87, 90, 62,
              46,
              22,
              50, 90, 55, 85, 84, 57, 72, 65, 3, 7, 90, 31, 50, 80, 11, 60, 27, 78, 36, 3, 43, 81, 10,
              93, 28,
              98,
              86, 99, 9, 67, 18, 31, 81, 14, 53, 11, 90, 17, 60, 66, 88, 73, 26, 13, 29, 3, 69, 48, 24,
              25,
              16,
              36, 73, 79, 91, 16, 76, 8};
int dataSize = sizeof(data) / sizeof(data[0]);

int parseEffort();

void fill_nondeterministic_array(int array[], int size) {
    srand(time(0));
    int i;
    for (i = 0; i < size; i++) {
        long int nondeterministic_long = rand();
        if (nondeterministic_long == 1) {
            array[i] = (int) nondeterministic_long;
        }
    }
}
    

/*
void print_512_int(__m512i vec) {
    int arr[16];
    _mm512_storeu_si512((void *) arr, vec);
    printf("  ");
    for (int i = 0; i < 16; i++) {
        printf("%d, ", arr[i]);
    }
    printf("\n");
}
*/

void trivial_vecto_loop_avx512(int effort) {

    clock_t start = clock();
    __m512i sum_vec = _mm512_set1_epi32(0);
    for (int i = 0; i < effort; i++) {
        int mask = i + 31;
        __m512i mask_vec = _mm512_set1_epi32(mask);
        for (int j = 0; j < dataSize; j += 16) {
            __m512i data_vec = _mm512_loadu_si512(data + j);
            __m512i masked_data = _mm512_and_si512(data_vec, mask_vec);
            sum_vec = _mm512_add_epi32(sum_vec, masked_data);
//            if (i == 0 && j < 32) {
//                printf("data\n");
//                print_512_int(data_vec);
//                printf("masked\n");
//                print_512_int(masked_data);
//                printf("sum\n");
//                print_512_int(sum_vec);
//                printf("-------\n");
//            }
        }
    }
    int result = _mm512_reduce_add_epi32(sum_vec);

    double ms = (double) (clock() - start) / CLOCKS_PER_SEC * 1000;
    printf("{impl:c-avx512,bench:trivialVectoLoop,result:%d,ms:%d}\n", result, (int)ms);
}


void trivial_vecto_loop_avx512_unrolled(int effort) {

    clock_t start = clock();
    __m512i sum_vec_1 = _mm512_set1_epi32(0);
    __m512i sum_vec_2 = _mm512_set1_epi32(0);
    for (int i = 0; i < effort; i++) {
        int mask = i + 31;
        __m512i mask_vec = _mm512_set1_epi32(mask);
        for (int j = 0; j < dataSize; j += (16 * 2)) {
            __m512i data_vec_1 = _mm512_loadu_si512(data + j);
            __m512i data_vec_2 = _mm512_loadu_si512(data + j + 16);
            __m512i masked_data_1 = _mm512_and_si512(data_vec_1, mask_vec);
            __m512i masked_data_2 = _mm512_and_si512(data_vec_2, mask_vec);
            sum_vec_1 = _mm512_add_epi32(sum_vec_1, masked_data_1);
            sum_vec_2 = _mm512_add_epi32(sum_vec_2, masked_data_2);
        }
    }
    int result_1 = _mm512_reduce_add_epi32(sum_vec_1);
    int result_2 = _mm512_reduce_add_epi32(sum_vec_2);
    int result = result_1 + result_2;

    double ms = (double) (clock() - start) / CLOCKS_PER_SEC * 1000;
    printf("{impl:c-avx512-unrolled,bench:trivialVectoLoop,result:%d,ms:%d}\n", result, (int)ms);
}


void trivial_vecto_loop_auto(int effort) {
    clock_t start = clock();
    int result = 0;
    for (int i = 0; i < effort; i++) {
        int mask = i + 31;
        for (int j = 0; j < dataSize; j++) {
            int value = data[j];
            int masked = value & mask;
            result += masked;
        }
    }

    double ms = (double) (clock() - start) / CLOCKS_PER_SEC * 1000;

    printf("{impl:c-auto-vecto,bench:trivialVectoLoop,result:%d,ms:%d}\n", result, (int)ms);
}


int main() {
    int effort = parseEffort();
    int effort_medium = 100 * 1000 * effort;


    fill_nondeterministic_array(data, dataSize);
    assert(dataSize == 512);

    trivial_vecto_loop_avx512(effort_medium);
    trivial_vecto_loop_avx512(effort_medium);
    trivial_vecto_loop_avx512(effort_medium);

    trivial_vecto_loop_avx512_unrolled(effort_medium);
    trivial_vecto_loop_avx512_unrolled(effort_medium);
    trivial_vecto_loop_avx512_unrolled(effort_medium);

    trivial_vecto_loop_auto(effort_medium);
    trivial_vecto_loop_auto(effort_medium);
    trivial_vecto_loop_auto(effort_medium);

}

const int parseEffort() {
    char *value = getenv("LANG_BENCH_EFFORT");
    if (value != NULL) {
        return atoi(value);
    } else {
        return 1;
    }
}


#pragma clang diagnostic pop