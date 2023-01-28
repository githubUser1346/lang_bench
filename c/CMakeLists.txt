cmake_minimum_required(VERSION 3.23)
project(c17_simon C)

set(CMAKE_C_STANDARD 17)
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -S -march=native -Os")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=native -O3 -funroll-all-loops")

add_executable(c17_simon main.c)
