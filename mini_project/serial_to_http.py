#!/usr/bin/env python3
"""串口 JSON -> JavaWeb 后端 HTTP 转发桥。

MCU 在串口打印一行行 JSON，例如：
{"deviceId":"sensor-001","temperature":25.3,"humidity":58.0,"pir":1,"mq135":132.5}

用法:
  pip install pyserial requests
  python serial_to_http.py --port COM3 --baud 115200 --host 127.0.0.1 --key env-monitor-2026
"""

from __future__ import annotations

import argparse
import json
import sys
import time

try:
    import serial
except ImportError:
    print("请先安装: pip install pyserial", file=sys.stderr)
    raise

try:
    import requests
except ImportError:
    print("请先安装: pip install requests", file=sys.stderr)
    raise


REQUIRED_KEYS = ("deviceId", "temperature", "humidity", "pir", "mq135")


def extract_json(line: str) -> dict | None:
    line = line.strip()
    if not line.startswith("{"):
        return None
    try:
        obj = json.loads(line)
    except json.JSONDecodeError:
        return None
    if not isinstance(obj, dict):
        return None
    if not all(k in obj for k in REQUIRED_KEYS):
        return None
    return obj


def upload(url: str, key: str, payload: dict) -> bool:
    headers = {"Content-Type": "application/json"}
    if key:
        headers["X-Device-Key"] = key
    try:
        resp = requests.post(url, headers=headers, json=payload, timeout=5)
        body = resp.json()
        ok = resp.ok and body.get("code") == 0
        print(f"[{'OK' if ok else 'ERR'}] {resp.status_code} {body.get('message')} "
              f"t={payload.get('temperature')} h={payload.get('humidity')} "
              f"pir={payload.get('pir')} mq={payload.get('mq135')}")
        return ok
    except Exception as exc:  # noqa: BLE001 — 桥接工具允许打印错误
        print(f"[FAIL] {exc}")
        return False


def main() -> int:
    ap = argparse.ArgumentParser(description="Serial JSON to EnvMonitor backend")
    ap.add_argument("--port", required=True, help="串口，如 COM3")
    ap.add_argument("--baud", type=int, default=115200)
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port-http", type=int, default=8080)
    ap.add_argument("--key", default="env-monitor-2026")
    ap.add_argument("--device", default=None, help="覆盖 JSON 中的 deviceId")
    args = ap.parse_args()

    url = f"http://{args.host}:{args.port_http}/api/device/data"
    print(f"Listening {args.baud} on {args.port} -> {url}")

    ser = serial.Serial(args.port, args.baud, timeout=1)
    try:
        while True:
            raw = ser.readline()
            if not raw:
                continue
            try:
                line = raw.decode("utf-8", errors="ignore")
            except Exception:
                continue
            payload = extract_json(line)
            if not payload:
                continue
            if args.device:
                payload["deviceId"] = args.device
            upload(url, args.key, payload)
            time.sleep(0.05)
    except KeyboardInterrupt:
        print("\nbye")
    finally:
        ser.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
