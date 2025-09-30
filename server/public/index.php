<?php

/**
 * License Server - Entry Point
 * Routes incoming requests to appropriate controllers
 */

require_once __DIR__ . '/../vendor/autoload.php';

// Load environment variables
$envFile = __DIR__ . '/../.env';
if (file_exists($envFile)) {
    $lines = file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        if (strpos(trim($line), '#') === 0) {
            continue;
        }
        list($name, $value) = explode('=', $line, 2);
        $_ENV[trim($name)] = trim($value);
    }
}

// Load configuration
$config = require __DIR__ . '/../config/config.php';
$routes = require __DIR__ . '/../config/routes.php';

// Set error reporting based on environment
if ($config['app']['debug']) {
    error_reporting(E_ALL);
    ini_set('display_errors', '1');
} else {
    error_reporting(0);
    ini_set('display_errors', '0');
}

// Get request method and URI
$method = $_SERVER['REQUEST_METHOD'];
$uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);

// Simple router
$handler = null;
foreach ($routes as $group => $groupRoutes) {
    foreach ($groupRoutes as $route => $routeHandler) {
        list($routeMethod, $routePath) = explode(' ', $route);

        // Convert route pattern to regex
        $pattern = preg_replace('/:\w+/', '([^/]+)', $routePath);
        $pattern = '#^' . $pattern . '$#';

        if ($method === $routeMethod && preg_match($pattern, $uri, $matches)) {
            $handler = $routeHandler;
            array_shift($matches); // Remove full match
            break 2;
        }
    }
}

// Execute handler or return 404
if ($handler) {
    list($controllerClass, $action) = $handler;
    $controller = new $controllerClass();
    echo $controller->$action(...$matches);
} else {
    http_response_code(404);
    echo json_encode(['error' => 'Not Found']);
}