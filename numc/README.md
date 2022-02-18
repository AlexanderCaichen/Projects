# numc

Functions that allow one to perform arithmetic operations between multiple "matrix" structs (see src/matrix.h for details).
Programmed entirely in C, the functions utilize SSE intrinsics, chunking, pragmas, loop unrolling, and loop caching to decrease the runtime of calculation-intensive functions.
Operations such as computing a matrix to some power can be sped up to more than 700x their naive implementations. 
