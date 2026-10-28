
import time
import statistics
import requests
import os
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = os.getenv("BASE_URL", "http://127.0.0.1:8080").rstrip("/")
ENDPOINT = "/api/books"
TOTAL_REQUESTS = 100
CONCURRENT_USERS = 10

def send_request(_):
    start = time.perf_counter()
    try:
        response = requests.get(
            BASE_URL + ENDPOINT,
            timeout=15
        )
        elapsed = (time.perf_counter() - start) * 1000
        return {
            "status": response.status_code,
            "latency_ms": elapsed,
            "error": None
        }
    except requests.RequestException as exc:
        elapsed = (time.perf_counter() - start) * 1000
        return {
            "status": None,
            "latency_ms": elapsed,
            "error": str(exc)
        }

def main():
    start = time.perf_counter()
    results = []

    with ThreadPoolExecutor(max_workers=CONCURRENT_USERS) as pool:
        futures = [
            pool.submit(send_request, i)
            for i in range(TOTAL_REQUESTS)
        ]
        for future in as_completed(futures):
            results.append(future.result())

    duration = time.perf_counter() - start
    successful = [r for r in results if r["status"] == 200]
    latencies = [r["latency_ms"] for r in results]

    print(f"Total requests: {len(results)}")
    print(f"Successful (HTTP 200): {len(successful)}")
    print(f"Failed/non-200: {len(results) - len(successful)}")
    print(f"Error rate: {(len(results) - len(successful)) / len(results) * 100:.2f}%")
    print(f"Duration: {duration:.2f} seconds")
    print(f"Throughput: {len(results) / duration:.2f} requests/second")
    print(f"Average latency: {statistics.mean(latencies):.2f} ms")
    print(f"Median latency: {statistics.median(latencies):.2f} ms")
    print(f"Max latency: {max(latencies):.2f} ms")

if __name__ == "__main__":
    main()
