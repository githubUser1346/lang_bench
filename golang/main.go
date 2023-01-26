package main

import (
	"encoding/json"
	"fmt"
	jsoniter "github.com/json-iterator/go"
	"log"
	"math/rand"
	"os"
	"runtime"
	"strconv"
	"time"
)

type BenchResult struct {
	Impl   string `json:"impl"`
	Bench  string `json:"bench"`
	Result string `json:"result"`
	Ms     int    `json:"ms"`
}

var effort = parseEffort()
var effortSmall = 10_000 * effort
var effortMedium = 100_000 * effort
var effortBig = 1000_000 * effort

func main() {
	fmt.Printf("# effort: %d\n", effort)
	for i := 0; i < 5; i++ {
		var nondeterministicData = nondeterministicArray()

		benchJsoniterSer(nondeterministicData, "json-ser", effortSmall)
		benchJsoniterDeser(nondeterministicData, "json-deser", effortSmall)
		benchItoa(nondeterministicData, "itoa", effortBig)

		benchNonVectoLoop(nondeterministicData, "nonVectoLoop", effortBig)
		benchComplexAutoVectoLoop(nondeterministicData, "complexVectoLoop", effortBig)
		benchTrivialAutoVectoLoop(nondeterministicData, "trivialVectoLoop", effortMedium)

		benchBranchingNonVectoLoop(nondeterministicData, "branchingNonVectoLoop", effortMedium)
		benchBranchingAutoVectoLoop(nondeterministicData, "branchingVectoLoop", effortMedium)
	}
}

func benchNonVectoLoop(nondeterministicData [512]int32, bench string, iterCount int) {
	var counter = 0
	var result int64 = 0

	runtime.GC()
	var t0 = time.Now().UnixMilli()
	for _, i := range nondeterministicData {
		for _, j := range nondeterministicData {
			for _, k := range nondeterministicData {
				for _, l := range nondeterministicData {
					result += (int64(i*j+k*l) + result) & 1023
				}
				counter += len(nondeterministicData)
				if counter >= iterCount {
					var t1 = time.Now().UnixMilli()
					printResult(bench, strconv.FormatInt(result, 10), t1, t0)
					return
				}
			}
		}
	}
	panic("unexpected result")
}

func benchComplexAutoVectoLoop(nondeterministicData [512]int32, bench string, iterCount int) {
	var counter = 0
	var result int64 = 0

	runtime.GC()
	var t0 = time.Now().UnixMilli()
	for _, i := range nondeterministicData {
		for _, j := range nondeterministicData {
			for _, k := range nondeterministicData {
				for _, l := range nondeterministicData {
					result += (int64(i*j+k*l) + 7) & 31
				}
				counter += len(nondeterministicData)
				if counter >= iterCount {
					var t1 = time.Now().UnixMilli()
					printResultFull(bench, "go-auto-vecto", strconv.FormatInt(result, 10), t1, t0)
					return
				}
			}
		}
	}
	panic("unexpected result")
}

func benchTrivialAutoVectoLoop(nondeterministicData [512]int32, bench string, iterCount int) {
	var result int32 = 0
	runtime.GC()
	var t0 = time.Now().UnixMilli()
	for i := 0; i < iterCount; i++ {
		var mask = int32(i + 31)
		for j := 0; j < len(nondeterministicData); j++ {
			result += nondeterministicData[j] & mask
		}
	}
	var t1 = time.Now().UnixMilli()
	printResultFull(bench, "go-auto-vecto", strconv.Itoa(int(result)), t1, t0)
}

/*

fun benchTrivialAutoVectoLoop(iterCount: Int): List<String> {
    var result = 0
    for (i in 0 until iterCount) {
        val randomStart = result % 5
        // This is our trivial loop. This is just a summation
        for (j in randomStart until nondeterministicData.size) {
            result += nondeterministicData[j]
        }
        result %= 1000
    }
    return listOf(result.toString())
}

*/

type DummyMessage struct {
	Id   int64
	Vec  []int64
	Map  map[string]int64
	Junk string
}

var message = DummyMessage{
	Id:   0,
	Vec:  []int64{111},
	Map:  map[string]int64{"key": 222},
	Junk: "",
}

func benchJsoniterSer(nondeterministicData [512]int32, bench string, iterCount int) {
	var result = int64(nondeterministicData[0])
	runtime.GC()
	var t0 = time.Now().UnixMilli()
	for j := 0; j < iterCount; j++ {
		message.Id = result
		encoded, _ := jsoniter.Marshal(message)
		result += int64(len(encoded))
	}

	var t1 = time.Now().UnixMilli()

	printResultFull(bench, "go-jsoniter", strconv.FormatInt(result, 10), t1, t0)
}

/*
	func benchJsonSer(nondeterministicData [512]int32, bench string, iterCount int) {
		var result = int64(nondeterministicData[0])
		runtime.GC()
		var t0 = time.Now().UnixMilli()
		for j := 0; j < int(iterCount); j++ {
			message.Id = result
			encoded, _ := json.Marshal(message)
			result += int64(len(encoded))
		}

		var t1 = time.Now().UnixMilli()

		printResultFull(bench, "go-json", strconv.FormatInt(result, 10), t1, t0)
	}
*/
const DeserJsonSpaced = `{"id":1 , "map" : { "uno" : 11 , "dos" : 222 } , "vec" : [ 1234 , 12345 ],"junk":"hd83hd89" }`

func benchJsoniterDeser(nondeterministicData [512]int32, bench string, iterCount int) {
	var result = int64(nondeterministicData[0])
	runtime.GC()
	var t0 = time.Now().UnixMilli()
	var encoded = []byte(DeserJsonSpaced)
	var zeroAscii byte = '0'

	for j := 0; j < iterCount; j++ {
		encoded[6] = zeroAscii + byte(result%8)
		decoded := DummyMessage{}
		_ = jsoniter.Unmarshal(encoded, &decoded)
		w := int64(len(decoded.Junk))
		x := decoded.Id
		y := decoded.Vec[0]
		z := decoded.Map["uno"]
		result += w + x + y + z
	}

	var t1 = time.Now().UnixMilli()

	printResultFull(bench, "go-jsoniter", strconv.FormatInt(result, 10), t1, t0)
}

func benchItoa(nondeterministicData [512]int32, bench string, iterCount int) {

	var result = nondeterministicData[0]
	runtime.GC()
	var t0 = time.Now().UnixMilli()
	for i := 0; i < iterCount; i++ {
		result %= 1_000_000_000
		result += int32(len(strconv.Itoa(int(result))))
	}
	var t1 = time.Now().UnixMilli()

	printResult(bench, strconv.FormatInt(int64(result), 10), t1, t0)
}
func benchBranchingNonVectoLoop(nondeterministicData [512]int32, bench string, iterCount int) {
	var result = 0
	runtime.GC()
	var t0 = time.Now().UnixMilli()
	for i := 0; i < iterCount; i++ {

		for _, value := range nondeterministicData {
			if result > 100 {
				result = 0
			}
			if value <= 50 {
				result += 10
			} else {
				result += 1
			}
		}
	}
	var t1 = time.Now().UnixMilli()
	printResult(bench, strconv.FormatInt(int64(result), 10), t1, t0)
}

func benchBranchingAutoVectoLoop(nondeterministicData [512]int32, bench string, iterCount int) {
	var result int64 = 0
	runtime.GC()
	var t0 = time.Now().UnixMilli()
	for i := 0; i < iterCount; i++ {
		for _, value := range nondeterministicData {
			if value <= 50 {
				result += 2
			} else {
				result += 1
			}
		}
	}
	var t1 = time.Now().UnixMilli()
	printResultFull(bench, "go-auto-vecto", strconv.FormatInt(result, 10), t1, t0)
}

func printResult(bench string, result string, t1 int64, t0 int64) {
	printResultFull(bench, "go", result, t1, t0)
}
func printResultFull(bench string, impl string, result string, t1 int64, t0 int64) {
	benchResult := &BenchResult{
		Impl:   impl,
		Bench:  bench,
		Result: result,
		Ms:     int(t1 - t0),
	}
	jsonResult, err := json.Marshal(benchResult)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s\n", jsonResult)
}

func nondeterministicArray() [512]int32 {
	var result = [512]int32{36, 31, 68, 73, 91, 33, 4, 30, 94, 28, 82, 46, 87, 89, 47, 63, 46, 97, 74, 60, 61, 24, 11, 72, 24, 17, 22, 9, 65, 81, 71, 17, 88, 1, 20, 70, 94, 52, 94, 22, 95, 84, 93, 65, 10, 50, 99, 73, 35, 53, 93, 46, 33, 85, 81, 52, 22, 91, 87, 70, 60, 94, 80, 59, 12, 44, 43, 68, 49, 33, 21, 40, 51, 95, 81, 84, 18, 2, 94, 34, 98, 95, 44, 10, 4, 35, 97, 57, 2, 98, 58, 4, 52, 15, 40, 20, 84, 48, 60, 21, 71, 47, 53, 38, 95, 69, 58, 32, 66, 37, 72, 29, 32, 63, 1, 94, 8, 67, 88, 77, 51, 77, 71, 83, 62, 33, 32, 89, 5, 17, 81, 37, 9, 32, 9, 7, 83, 28, 60, 14, 9, 86, 43, 5, 82, 68, 82, 99, 98, 96, 38, 54, 97, 51, 34, 90, 47, 89, 12, 9, 16, 39, 41, 66, 99, 27, 78, 38, 50, 50, 67, 39, 70, 78, 58, 21, 47, 11, 46, 41, 21, 96, 24, 44, 80, 95, 56, 13, 94, 79, 43, 48, 86, 64, 9, 24, 31, 49, 30, 58, 43, 38, 31, 39, 65, 84, 30, 3, 3, 20, 73, 63, 84, 76, 14, 24, 77, 51, 50, 22, 7, 11, 58, 40, 72, 35, 76, 20, 44, 94, 6, 3, 95, 50, 32, 4, 81, 71, 67, 45, 69, 12, 21, 78, 49, 96, 50, 3, 47, 53, 35, 47, 75, 40, 24, 34, 68, 38, 48, 2, 69, 22, 19, 24, 27, 27, 70, 41, 62, 55, 91, 3, 72, 16, 52, 40, 55, 68, 79, 13, 71, 27, 64, 24, 63, 70, 60, 48, 36, 83, 16, 86, 99, 69, 15, 5, 24, 30, 63, 98, 89, 51, 9, 40, 57, 67, 96, 32, 42, 60, 79, 86, 57, 48, 65, 56, 69, 98, 43, 21, 92, 20, 38, 40, 24, 62, 62, 5, 54, 50, 58, 53, 32, 31, 17, 11, 20, 30, 95, 53, 21, 47, 86, 51, 11, 69, 95, 48, 56, 91, 49, 29, 12, 95, 19, 55, 79, 42, 50, 12, 49, 20, 57, 20, 11, 29, 71, 64, 68, 86, 50, 65, 60, 90, 93, 75, 65, 4, 6, 67, 95, 34, 42, 54, 13, 58, 52, 5, 11, 79, 37, 41, 83, 9, 96, 43, 15, 32, 30, 35, 29, 98, 53, 80, 74, 89, 65, 30, 14, 69, 73, 82, 87, 61, 50, 44, 53, 12, 39, 37, 47, 21, 82, 66, 8, 49, 99, 52, 97, 66, 51, 33, 35, 19, 45, 28, 86, 90, 20, 43, 24, 37, 39, 83, 27, 2, 12, 4, 77, 87, 90, 62, 46, 22, 50, 90, 55, 85, 84, 57, 72, 65, 3, 7, 90, 31, 50, 80, 11, 60, 27, 78, 36, 3, 43, 81, 10, 93, 28, 98, 86, 99, 9, 67, 18, 31, 81, 14, 53, 11, 90, 17, 60, 66, 88, 73, 26, 13, 29, 3, 69, 48, 24, 25, 16, 36, 73, 79, 91, 16, 76, 8}
	var random = rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := 0; i < len(result); i++ {
		var nondeterministicLong = random.Int63()
		if nondeterministicLong == 1 {
			// Statistically, that will branch will never be taken,
			// yet the compilers will have to assume nondeterminism.
			// If ever in a million year this is invoked... benchmarks will fail during the result verification in BenchRunner.
			result[i] = int32(nondeterministicLong)
		}
	}
	return result
}

func parseEffort() int {
	envVar := os.Getenv("LANG_BENCH_EFFORT")
	intValue, err := strconv.Atoi(envVar)
	if err != nil {
		return 1
	} else {
		return intValue
	}
}
