#!/bin/bash

# Docker Helper Script
# Convenient commands for managing the license server containers

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function print_usage() {
    echo "Docker Helper Script for License Server"
    echo ""
    echo "Usage: ./docker-helper.sh <command>"
    echo ""
    echo "Commands:"
    echo "  start       Start all containers"
    echo "  stop        Stop all containers"
    echo "  restart     Restart all containers"
    echo "  logs        View logs (follow mode)"
    echo "  logs-web    View web server logs"
    echo "  logs-db     View database logs"
    echo "  shell       Open shell in web container"
    echo "  shell-db    Open MySQL shell in database container"
    echo "  migrate     Run database migrations"
    echo "  clean       Stop containers and remove volumes"
    echo "  rebuild     Rebuild and restart containers"
    echo "  status      Show container status"
    echo ""
}

function start_containers() {
    echo -e "${GREEN}Starting containers...${NC}"
    docker-compose up -d
    echo -e "${GREEN}Containers started!${NC}"
    docker-compose ps
}

function stop_containers() {
    echo -e "${YELLOW}Stopping containers...${NC}"
    docker-compose down
    echo -e "${GREEN}Containers stopped!${NC}"
}

function restart_containers() {
    echo -e "${YELLOW}Restarting containers...${NC}"
    docker-compose restart
    echo -e "${GREEN}Containers restarted!${NC}"
}

function view_logs() {
    docker-compose logs -f
}

function view_logs_web() {
    docker-compose logs -f web
}

function view_logs_db() {
    docker-compose logs -f db
}

function open_shell() {
    docker-compose exec web bash
}

function open_db_shell() {
    docker-compose exec db mysql -u"${DB_USERNAME:-license_user}" -p"${DB_PASSWORD:-license_password}" "${DB_DATABASE:-license_system}"
}

function run_migrations() {
    echo -e "${GREEN}Running database migrations...${NC}"
    docker-compose exec web php migrate.php
    echo -e "${GREEN}Migrations completed!${NC}"
}

function clean_containers() {
    echo -e "${RED}WARNING: This will remove all containers and volumes!${NC}"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down -v
        echo -e "${GREEN}Cleanup completed!${NC}"
    else
        echo -e "${YELLOW}Cleanup cancelled.${NC}"
    fi
}

function rebuild_containers() {
    echo -e "${YELLOW}Rebuilding containers...${NC}"
    docker-compose down
    docker-compose build --no-cache
    docker-compose up -d
    echo -e "${GREEN}Rebuild completed!${NC}"
    docker-compose ps
}

function show_status() {
    docker-compose ps
}

# Main command dispatcher
case "${1:-}" in
    start)
        start_containers
        ;;
    stop)
        stop_containers
        ;;
    restart)
        restart_containers
        ;;
    logs)
        view_logs
        ;;
    logs-web)
        view_logs_web
        ;;
    logs-db)
        view_logs_db
        ;;
    shell)
        open_shell
        ;;
    shell-db)
        open_db_shell
        ;;
    migrate)
        run_migrations
        ;;
    clean)
        clean_containers
        ;;
    rebuild)
        rebuild_containers
        ;;
    status)
        show_status
        ;;
    *)
        print_usage
        exit 1
        ;;
esac