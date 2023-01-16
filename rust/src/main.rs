#![allow(unused_variables)]
#![allow(dead_code)]
#![allow(arithmetic_overflow)]
#![allow(unreachable_patterns)]
#![allow(unused_mut)]

extern crate byteorder;

use std::borrow::Cow;
use std::collections::HashMap;
use std::env;
use std::time::{Duration, Instant};

use rand::Rng;
use serde::{Deserialize, Serialize};

fn main() {
    let effort: i32 = parse_effort();
    let effort_small: i32 = 10_000 * effort;
    let effort_medium: i32 = 100_000 * effort;
    let effort_big: i32 = 1_000_000 * effort;

    println!("# effort: {}", effort);

    for i in 1..5 {
        let nondeterministic_data: Vec<i32> = nondeterministic_array();
        let mut results: Vec<String> = vec!();

        results.push(bench_json_ser(&nondeterministic_data, "json-ser", effort_small));
        results.push(bench_json_deser(&nondeterministic_data, "json-deser", effort_small));
        results.push(bench_itoa(&nondeterministic_data, "itoa", effort_big));

        results.push(bench_non_vecto_loop(&nondeterministic_data, "nonVectoLoop", effort_big));
        results.push(bench_trivial_auto_vecto_loop(&nondeterministic_data, "trivialVectoLoop", effort_medium));
        results.push(bench_complex_auto_vecto_loop(&nondeterministic_data, "complexVectoLoop", effort_big));

        results.push(bench_branching_non_vecto_loop(&nondeterministic_data, "branchingNonVectoLoop", effort_medium));
        results.push(bench_branching_auto_vecto_loop(&nondeterministic_data, "branchingVectoLoop", effort_medium));

        if 1 < i {
            for r in results {
                println!("{}", r);
            }
        }
    }
}

#[derive(Debug, Deserialize, Serialize)]
struct BenchResult {
    r#impl: String,
    bench: String,
    result: String,
    ms: i64,
}


fn bench_non_vecto_loop(nondeterministic_data: &Vec<i32>, bench: &str, iter_count: i32) -> String {
    let mut counter = 0;
    let mut result: i64 = 0;

    let start = Instant::now();
    // Because result is a dependency on each iteration, this is assumed to be non-vectorizable by the compiler.
    // The performance of non vectorizable loops tends to change less across languages.

    //This four nested loops provides lots of random data for cheap since
    // 1- Everything is precomputed
    // 2- It uses the 512 bytes all the times.
    // A large machine EFFORT would shadow the language logic EFFORT.
    for i in nondeterministic_data {
        for j in nondeterministic_data {
            for k in nondeterministic_data {
                for l in nondeterministic_data {
                    let i64 = *i as i64;
                    let j64 = *j as i64;
                    let k64 = *k as i64;
                    let l64 = *l as i64;
                    result += (i64 * j64 + k64 * l64 + result) | 1023;
                }
                counter += nondeterministic_data.len();
                if counter >= iter_count as usize {
                    let elapsed = start.elapsed();
                    let result_string = result.to_string();
                    return to_json(bench, result_string, elapsed);
                }
            }
        }
    }
    panic!();
}

fn bench_complex_auto_vecto_loop(nondeterministic_data: &Vec<i32>, bench: &str, iter_count: i32) -> String {
    let mut counter = 0;
    let mut result: i64 = 0;

    let start = Instant::now();
    for i in nondeterministic_data {
        for j in nondeterministic_data {
            for k in nondeterministic_data {
                for l in nondeterministic_data {
                    let i64 = *i as i64;
                    let j64 = *j as i64;
                    let k64 = *k as i64;
                    let l64 = *l as i64;
                    result += (i64 * j64 + k64 * l64 + 7) | 1023;
                }
                counter += nondeterministic_data.len();
                if counter >= iter_count as usize {
                    let elapsed = start.elapsed();
                    let result_string = result.to_string();
                    return to_json2(bench, "rust-auto-vecto", result_string, elapsed);
                }
            }
        }
    }
    panic!();
}

fn bench_trivial_auto_vecto_loop(nondeterministic_data: &Vec<i32>, bench: &str, iter_count: i32) -> String {
    let mut result: i32 = 0;

    let start = Instant::now();
    for _ in 0..iter_count {
        let random_start = result % 5;
        // This is our trivial loop. This is just a summation
        for i in (random_start) as usize..nondeterministic_data.len() {
            result += nondeterministic_data[i];
        }
        result %= 1000
    }

    let elapsed = start.elapsed();
    let result_string = result.to_string();
    return to_json2(bench, "rust-auto-vecto", result_string, elapsed);
}


fn to_json2(bench: &str, imp: &str, result: String, elapsed: Duration) -> String {
    let imp = imp.to_string();
    let result = result.to_string();
    let bench_result = BenchResult { r#impl: imp, bench: bench.to_string(), result, ms: elapsed.as_millis() as i64 };
    let json = serde_json::to_string(&bench_result).unwrap();
    return json;
}

fn to_json(bench: &str, result: String, elapsed: Duration) -> String {
    return to_json2(bench, "rust", result, elapsed);
}

fn bench_itoa(nondeterministic_data: &Vec<i32>, bench: &str, iter_count: i32) -> String {
    let start = Instant::now();
    let mut result: i64 = *nondeterministic_data.first().unwrap() as i64;
    let mut buffer = itoa::Buffer::new();
    for _ in 0..iter_count {
        result %= 1_000_000_000;
        let printed: &str = buffer.format(result);
        result += printed.len() as i64;
    }
    let elapsed = start.elapsed();

    return to_json(bench, result.to_string(), elapsed);
}


#[derive(Debug, Deserialize, Serialize)]
struct DummyMessage {
    id: i64,
    vec: Vec<i64>,
    map: HashMap<String, i64>,
    junk: String,
}

const DESER_STR: &str = r###"{"id":1 , "map" : { "uno" : 11 , "dos" : 222 } , "vec" : [ 1234 , 12345 ], "junk":"hd83hd89" }"###;

fn bench_json_deser(nondeterministic_data: &Vec<i32>, bench: &str, iter_count: i32) -> String {
    let mut result: i64 = *nondeterministic_data.first().unwrap() as i64;

    let string: String = DESER_STR.to_string();
    let mut vec: Vec<u8> = string.into_bytes();

    let start = Instant::now();

    const ZERO_ASCII: u8 = 48;
    for _ in 0..iter_count {
        vec[6] = ZERO_ASCII + u8::try_from(result % 8).unwrap();
        let encoded_string_mod: Cow<str> = String::from_utf8_lossy(&vec);
        let decoded: DummyMessage = serde_json::from_str(&encoded_string_mod).unwrap();
        let w: i64 = decoded.junk.len() as i64;
        let x: i64 = decoded.id;
        let y: i64 = decoded.vec[0];
        let z: i64 = *decoded.map.get("uno").unwrap();
        result += w + x + y + z;
    }
    let elapsed = start.elapsed();
    return to_json2(bench, "rust-serde", result.to_string(), elapsed);
}

const BRANCH_TEST_STRING: &str = "a11aaaa222222zzzzzzzzzzz3333333333";

fn bench_branching_non_vecto_loop(nondeterministic_data: &Vec<i32>, bench: &str, iter_count: i32) -> String {
    let mut result: i64 = 0;
    let start = Instant::now();
    for i in 0..iter_count {
        for value in nondeterministic_data {
            if result > 100 {
                result = 0;
            }
            if *value <= 50 {
                result += 10;
            } else {
                result += 1;
            }
        }
    }

    let elapsed = start.elapsed();
    return to_json2(bench, "rust", result.to_string(), elapsed);
}

fn bench_branching_auto_vecto_loop(nondeterministic_data: &Vec<i32>, bench: &str, iter_count: i32) -> String {
    let mut result: i64 = 0;
    let start = Instant::now();
    for i in 0..iter_count {
        for value in nondeterministic_data {
            if *value <= 50 {
                result += 2;
            } else {
                result += 1;
            }
        }
    }

    let elapsed = start.elapsed();
    return to_json2(bench, "rust-auto-vecto", result.to_string(), elapsed);
}


fn build_byte_array() -> Vec<u8> {
    vec!(
        0, 0, 1, 2,
        0, 0, 1, 3,
        5, 7, 8, 8,
        4, 3, 6, 8,
        0, 4, 7, 8,
        9, 4, 7, 7,
        3, 6, 1, 8,
        0, 5, 7, 3,
        5, 7, 8, 4
    )
}

fn bench_json_ser(nondeterministic_data: &Vec<i32>, bench: &str, iter_count: i32) -> String {
    let mut map: HashMap<String, i64> = HashMap::new();
    map.insert("key".to_string(), 222);
    let vec = vec!(111);
    let junk = "".to_string();
    let mut message = DummyMessage {
        id: 1,
        vec,
        map,
        junk,
    };

    let mut result: i64 = *nondeterministic_data.first().unwrap() as i64;

    let start = Instant::now();
    for _ in 0..iter_count {
        message.id = result;
        let encoded: String = serde_json::to_string(&message).unwrap();
        let len = encoded.len();
        result += len as i64;
    }
    let elapsed = start.elapsed();
    return to_json2(bench, "rust-serde", result.to_string(), elapsed);
}

// fn bench_json_zero_deser(bench: &str, iter_count: i32){
//     let mut result: i64 = 1;
//
//     let encoded_string: String = r###"{"id":1 , "map" : { "uno" : 11 , "dos" : 222 } , "vec" : [ 1234 , 12345 ] }"###.to_string();
//     let mut encoded_bytes: Vec<u8> = encoded_string.into_bytes();
//
//     let start = Instant::now();
//
//     for _ in 0..iter_count {
//         // The modulo is used only to avoid overflows.
//         result = (result * 31) % 1_000_000;
//
//         encoded_bytes[6] = 48 + u8::try_from(result % 8).unwrap();
//         let encoded_string_mod: Cow<str> = String::from_utf8_lossy(&encoded_bytes);
//         let decoded: DummyMessage = serde_json::from_str(&encoded_string_mod).unwrap();
//         let x: i64 = decoded.id;
//         let y: i64 = decoded.vec[0];
//         let z: i64 = *decoded.map.get("uno").unwrap();
//         result += x + &y + &z;
//     }
//     let elapsed = start.elapsed();
//     to_json(bench, result.to_string(), elapsed);
// }

// fn bench_json_parallel(bench: &str, iter_count: i32) -> Vec<String> {
//     let mut children: Vec<JoinHandle<Vec<String>>> = Vec::new();
//
//     for _ in 0..NUM_THREADS {
//         let bench2: String = bench.to_string();
//         let child: JoinHandle<Vec<String>> = std::thread::spawn(move || {
//             return bench_json(&bench2, iter_count / NUM_THREADS);
//         });
//         children.push(child);
//     }
//
//     let mut results: Vec<String> = Vec::new();
//     for child in children {
//         let mut r: Vec<String> = child.join().expect("oops! the child thread panicked");
//         results.append(&mut r);
//     }
//     return results;
// }

fn temp(iter_count: i32) {
    let mut result: i64 = 1;

    let start = Instant::now();

    for i in 0..iter_count {}
    let elapsed = start.elapsed();
    to_json2("hash", "simd-tape", result.to_string(), elapsed);
}


fn nondeterministic_array() -> Vec<i32> {
    let mut result: Vec<i32> = vec!(36, 31, 68, 73, 91, 33, 4, 30, 94, 28, 82, 46, 87, 89, 47, 63, 46, 97, 74, 60, 61, 24, 11, 72, 24, 17, 22, 9, 65, 81, 71, 17, 88, 1, 20, 70, 94, 52, 94, 22, 95, 84, 93, 65, 10, 50, 99, 73, 35, 53, 93, 46, 33, 85, 81, 52, 22, 91, 87, 70, 60, 94, 80, 59, 12, 44, 43, 68, 49, 33, 21, 40, 51, 95, 81, 84, 18, 2, 94, 34, 98, 95, 44, 10, 4, 35, 97, 57, 2, 98, 58, 4, 52, 15, 40, 20, 84, 48, 60, 21, 71, 47, 53, 38, 95, 69, 58, 32, 66, 37, 72, 29, 32, 63, 1, 94, 8, 67, 88, 77, 51, 77, 71, 83, 62, 33, 32, 89, 5, 17, 81, 37, 9, 32, 9, 7, 83, 28, 60, 14, 9, 86, 43, 5, 82, 68, 82, 99, 98, 96, 38, 54, 97, 51, 34, 90, 47, 89, 12, 9, 16, 39, 41, 66, 99, 27, 78, 38, 50, 50, 67, 39, 70, 78, 58, 21, 47, 11, 46, 41, 21, 96, 24, 44, 80, 95, 56, 13, 94, 79, 43, 48, 86, 64, 9, 24, 31, 49, 30, 58, 43, 38, 31, 39, 65, 84, 30, 3, 3, 20, 73, 63, 84, 76, 14, 24, 77, 51, 50, 22, 7, 11, 58, 40, 72, 35, 76, 20, 44, 94, 6, 3, 95, 50, 32, 4, 81, 71, 67, 45, 69, 12, 21, 78, 49, 96, 50, 3, 47, 53, 35, 47, 75, 40, 24, 34, 68, 38, 48, 2, 69, 22, 19, 24, 27, 27, 70, 41, 62, 55, 91, 3, 72, 16, 52, 40, 55, 68, 79, 13, 71, 27, 64, 24, 63, 70, 60, 48, 36, 83, 16, 86, 99, 69, 15, 5, 24, 30, 63, 98, 89, 51, 9, 40, 57, 67, 96, 32, 42, 60, 79, 86, 57, 48, 65, 56, 69, 98, 43, 21, 92, 20, 38, 40, 24, 62, 62, 5, 54, 50, 58, 53, 32, 31, 17, 11, 20, 30, 95, 53, 21, 47, 86, 51, 11, 69, 95, 48, 56, 91, 49, 29, 12, 95, 19, 55, 79, 42, 50, 12, 49, 20, 57, 20, 11, 29, 71, 64, 68, 86, 50, 65, 60, 90, 93, 75, 65, 4, 6, 67, 95, 34, 42, 54, 13, 58, 52, 5, 11, 79, 37, 41, 83, 9, 96, 43, 15, 32, 30, 35, 29, 98, 53, 80, 74, 89, 65, 30, 14, 69, 73, 82, 87, 61, 50, 44, 53, 12, 39, 37, 47, 21, 82, 66, 8, 49, 99, 52, 97, 66, 51, 33, 35, 19, 45, 28, 86, 90, 20, 43, 24, 37, 39, 83, 27, 2, 12, 4, 77, 87, 90, 62, 46, 22, 50, 90, 55, 85, 84, 57, 72, 65, 3, 7, 90, 31, 50, 80, 11, 60, 27, 78, 36, 3, 43, 81, 10, 93, 28, 98, 86, 99, 9, 67, 18, 31, 81, 14, 53, 11, 90, 17, 60, 66, 88, 73, 26, 13, 29, 3, 69, 48, 24, 25, 16, 36, 73, 79, 91, 16, 76, 8);
    assert_eq!(result.len(), 512);
    let mut random = rand::thread_rng();
    for i in 0..512 {
        let nondeterministic_long: i64 = random.gen();
        if nondeterministic_long == 1 {
            // Statistically, that will branch will never be taken,
            // If ever this is invoked... the benchmark will fail during the result verification in BenchRunner.
            result[i] = nondeterministic_long as i32
        }
    }
    return result;
}

fn parse_effort() -> i32 {
    let effort_string = env::var("LANG_BENCH_EFFORT").unwrap_or("1".to_string());
    let parsed = effort_string.parse();
    return if parsed.is_ok() {
        parsed.unwrap()
    } else {
        1
    };
}


#[cfg(test)]
mod test {
    #[test]
    fn ff() {}
}