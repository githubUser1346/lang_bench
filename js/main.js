

for (let i = 0; i < 5; i++) {
  benchSumLoop("sumLoop", 1000 * 1000 * 1000);
  benchItoa("itoa", 100_000_000);
  benchIntSet("intSetReads",  100_000_000, 100);
  benchIntSet("intSetWrites",  10_000_000, 10_000_000);
  benchIntSetMultiThreaded();
}

function benchSumLoop(bench, iterCount) {
  const t0 = Date.now()

  let prevPrev = 1;
  let prev = 1;
  let sum = 2;

  for (let j = 0; j < iterCount; j++) {
    // The modulo is used only to avoid overflows.
    let next = (prevPrev + prev) % (1000 * 1000);
    prevPrev = prev;
    prev = next;
    sum += next;
  }
  const result = sum;

  const t1 = Date.now()

  console.log(JSON.stringify({impl: "javascript", bench: "sumLoop", result: result.toString(), ms: t1 - t0}))

}

function benchIntSet(bench, iterCount, maxSize) {

  const t0 = Date.now()

  let prevPrev = 1;
  let prev = 1;
  let intSet = new Set();

  for (let j = 0; j < iterCount; j++) {
    // The modulo is used only to avoid overflows.
    let next = (prevPrev + prev) % (1000 * 1000 * 1000);
    prevPrev = prev;
    prev = next;
    // The module is used to keep the set small, since we are not testing memory speed.
    intSet.add(next % maxSize)
  }
  let result = intSet.size;

  const t1 = Date.now()

  console.log(JSON.stringify({impl: "javascript", bench: bench, result: result.toString(), ms: t1 - t0}))

}

function benchIntSetMultiThreaded() {
  for (let j = 0; j < 10; j++) {
    new Worker(new URL("./worker.js", import.meta.url).href, { type: "module" });
  }
}

function benchItoa(bench, iterCount) {

  const t0 = Date.now()

  let prevPrev = 1;
  let prev = 1;
  let result = 0;

  for (let j = 0; j < iterCount; j++) {
    // The modulo is used only to avoid overflows.
    let next = (prevPrev + prev) % (1000 * 1000 * 1000);
    prevPrev = prev;
    prev = next;
    // The module is used to keep the set small, since we are not testing memory speed.
    result += String(next).length
  }

  const t1 = Date.now()

  console.log(JSON.stringify({impl: "javascript", bench: bench, result: result.toString(), ms: t1 - t0}))
}