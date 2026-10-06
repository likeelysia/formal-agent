@echo off
REM ============================================================
REM  Start local embedding service (llama.cpp + bge-small-zh)
REM  OpenAI-compatible endpoint: POST http://127.0.0.1:8090/v1/embeddings
REM  Stop: close the minimized window, or: taskkill /IM llama-server.exe /F
REM ============================================================
start "embedding" /min "F:\opc-tools\llama.cpp\llama-server.exe" -m "F:\opc-tools\llm-models\bge-small-zh-v1.5-q8_0.gguf" --embeddings --pooling cls -c 512 --host 127.0.0.1 --port 8090 -t 8
echo embedding service starting on http://127.0.0.1:8090
