const caseInsensitiveSort = (a, b) => {
  const lowerA = a.toLowerCase();
  const lowerB = b.toLowerCase();
  if (lowerA < lowerB) return -1;
  if (lowerA > lowerB) return 1;
  return 0;
};

const sortedGlobals = [...Object.getOwnPropertyNames(globalThis)].sort(caseInsensitiveSort);

print(sortedGlobals.join("\n"));
