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
benchIntSet("intSetWrites-parallel", 100_000_000, 100_000_000)

self.close();