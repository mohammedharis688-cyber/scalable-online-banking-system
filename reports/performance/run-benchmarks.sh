#!/bin/bash
set -e

# ==============================================================================
# Scalable Online Banking System — Apache JMeter Benchmark Automation
# UCS3513 System Design Laboratory
# Workloads: Low (10 users), Medium (25 users), High (50 users)
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLAN="${SCRIPT_DIR}/scalable-banking-benchmark.jmx"
HOST="${1:-localhost}"
PORT="${2:-8080}"

echo "=========================================================="
echo " Starting UCS3513 JMeter Performance Benchmarking Suite"
echo " Target Application: http://${HOST}:${PORT}"
echo "=========================================================="

mkdir -p "${SCRIPT_DIR}/raw"

JMETER_BIN="jmeter"
if [ -x "/home/devcontainers/apache-jmeter-5.6.3/bin/jmeter" ]; then
    JMETER_BIN="/home/devcontainers/apache-jmeter-5.6.3/bin/jmeter"
elif [ -x "$HOME/apache-jmeter-5.6.3/bin/jmeter" ]; then
    JMETER_BIN="$HOME/apache-jmeter-5.6.3/bin/jmeter"
fi

run_workload() {
    name=$1
    threads=$2
    rampup=$3
    loops=$4

    echo ""
    echo ">>> Running ${name} Workload: ${threads} concurrent threads, ${rampup}s ramp-up, ${loops} loops..."
    jtl_file="${SCRIPT_DIR}/raw/results-${name}.jtl"
    report_dir="${SCRIPT_DIR}/dashboard-${name}"

    rm -f "${jtl_file}"
    rm -rf "${report_dir}"

    "$JMETER_BIN" -n \
        -t "${PLAN}" \
        -Jhost="${HOST}" \
        -Jport="${PORT}" \
        -Jthreads="${threads}" \
        -Jrampup="${rampup}" \
        -Jloops="${loops}" \
        -l "${jtl_file}" \
        -e -o "${report_dir}"

    echo ">>> ${name} Workload finished! Report generated at: ${report_dir}/index.html"
}

# 1. Low Workload: 10 concurrent threads
run_workload "low" 10 5 20

# 2. Medium Workload: 25 concurrent threads
run_workload "medium" 25 10 20

# 3. High Workload: 50 concurrent threads
run_workload "high" 50 15 20

echo ""
echo "=========================================================="
echo " All JMeter benchmark workloads completed successfully!"
echo " Raw Results: ${SCRIPT_DIR}/raw/"
echo " HTML Dashboards: ${SCRIPT_DIR}/dashboard-*/"
echo "=========================================================="
