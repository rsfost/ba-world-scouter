/** A shared 1Hz clock. Ages in the table are relative to it, so every row
 *  re-renders its label from one timer instead of holding its own. */
let now = $state(Date.now());

setInterval(() => {
  if (document.visibilityState === 'visible') now = Date.now();
}, 1000);

document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') now = Date.now();
});

/** Current wall clock in unix seconds, reactive. */
export function nowSeconds(): number {
  return now / 1000;
}
