#include "matrix.h"
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <omp.h>

// Include SSE intrinsics
#if defined(_MSC_VER)
#include <intrin.h>
#elif defined(__GNUC__) && (defined(__x86_64__) || defined(__i386__))
#include <immintrin.h>
#include <x86intrin.h>
#endif

/* Generates a random double between low and high */
double rand_double(double low, double high) {
    double range = (high - low);
    double div = RAND_MAX / range;
    return low + (rand() / div);
}

/* Generates a random matrix */
void rand_matrix(matrix *result, unsigned int seed, double low, double high) {
    srand(seed);
    for (int i = 0; i < result->rows; i++) {
        for (int j = 0; j < result->cols; j++) {
            set(result, i, j, rand_double(low, high));
        }
    }
}

/*
 * Returns the double value of the matrix at the given row and column.
 * Matrix is in ROW-MAJOR order.
    Ex: 123
        456
        789
        (123456789)
 */
double get(matrix *mat, int row, int col) {
    //double* data = mat->data;
    //return *((mat->data) + col*sizeof(double) + (mat->cols)*row*sizeof(double));
    return (mat->data)[col + (mat->cols)*row];
}

/*
 * Sets the value at the given row and column to val.
 * Matrix is in row-major order.
 */
void set(matrix *mat, int row, int col, double val) {
    (mat->data)[col + (mat->cols)*row] = val;
}

/*
 * Allocates space for a matrix struct pointed to by the double pointer mat with
 * `rows` rows and `cols` columns.
 * Initialize all entries to be zeros.
 * Sets `parent` should be set to NULL to indicate that this matrix is not a slice.
 * Returns -1 if either `rows` or `cols` or both have invalid values.
 * Returns -2 if any call to allocate memory in this function fails.
 * Return 0 upon success.
 */
int allocate_matrix(matrix **mat, int rows, int cols) {
    if (rows <= 0 || cols <= 0) {
        return -1;
    }
    
    matrix* tempMatrix = (matrix*)malloc(sizeof(matrix));
    if (tempMatrix == NULL) {
        return -2;
    }
    
    tempMatrix->data = (double*)calloc(rows * cols, sizeof(double));
    if (tempMatrix->data == NULL) {
        free(tempMatrix);
        return -2;
    }
    
    tempMatrix->rows = rows;
    tempMatrix->cols = cols;
    tempMatrix->parent = NULL;
    tempMatrix->ref_cnt = 1;
    
    *mat = tempMatrix;
    
    return 0;
}

/*
 * Frees `mat->data` if `mat` is not a slice and has no existing slices
 * Frees `mat->parent->data` if `mat` is the last existing slice of its parent matrix and its parent matrix has no other references (including itself).
 */
void deallocate_matrix(matrix *mat) {
    if (mat == NULL) {
        return;
    }
    if (mat->parent == NULL) {
        mat->ref_cnt -= 1;
        if (mat->ref_cnt == 0) {
            free(mat->data);
            free(mat);
        }
    }
    else {
        deallocate_matrix(mat->parent);
        //free(mat->data);
        free(mat);
    }
}

/*
 * Allocates space for a matrix struct pointed to by `mat` with `rows` rows and `cols` columns.
 * Its data points to the `offset`th entry of `from`'s data
 * Returns -1 if either `rows` or `cols` or both have invalid values.
 * Returns -2 if any call to allocate memory in this function fails.
 * Returns 0 upon success.
 */
int allocate_matrix_ref(matrix **mat, matrix *from, int offset, int rows, int cols) {
    if (rows <= 0 || cols <= 0) {
        return -1;
    }
    
    matrix* tempMatrix = (matrix*)malloc(sizeof(matrix));
    if (tempMatrix == NULL) {
        return -2;
    }
    
    tempMatrix->data = (from->data) + offset;
    
    tempMatrix->rows = rows;
    tempMatrix->cols = cols;
    
    tempMatrix->parent = from;
    from->ref_cnt += 1;
    
    //tempMatrix->ref_cnt = 1;
    *mat = tempMatrix;
    
    return 0;
}

/*
 * Sets all entries in mat to val. Note that the matrix is in row-major order.
 */
void fill_matrix(matrix *mat, double val) {
    __m256d temp = _mm256_set1_pd(val);
    int maxi = (mat->cols) * (mat->rows);
	int i;
	#pragma omp parallel
	{
    	#pragma omp for private(i) nowait
        for (i = 0; i < 16*(maxi/16); i+=16) {
            _mm256_storeu_pd((mat->data) + i, temp);
            _mm256_storeu_pd((mat->data) + i + 4, temp);
            _mm256_storeu_pd((mat->data) + i + 8, temp);
            _mm256_storeu_pd((mat->data) + i + 12, temp);
        }
    
        //divisible by 4, Unrolling
        #pragma omp for private(i) nowait
        for (i = 16*(maxi/16); i < 4*(maxi/4); i+=4) {
            _mm256_storeu_pd((mat->data) + i, temp);
        }
        //ending
        #pragma omp for private(i)
        for (i = 4*(maxi/4); i < maxi; i++) {
            (mat->data)[i] = val;
        }
	}
}

/*
 * Store the result of taking the absolute value element-wise to `result`.
 * Return 0 upon success.
 * Note that the matrix is in row-major order.
 */
int abs_matrix(matrix *result, matrix *mat) {
    //result will presist
    int maxi = (mat->cols) * (mat->rows);
    
    //__m256d mask = _mm256_castsi256_pd(_mm_set1_epi64x (0x7FFFFFFFFFFFFFFF));
    __m256d mask = _mm256_set1_pd(-0.0);

    __m256d temp;
    
    #pragma omp parallel 
	{
        //divisible by 16, Unrolling for better performance
        #pragma omp for nowait
        for (int i = 0; i < 16*(maxi/16); i+=16) {
            temp = _mm256_loadu_pd((mat->data) + i);
            temp = _mm256_andnot_pd(mask, temp);
            _mm256_storeu_pd((result->data) + i, temp);
            
            temp = _mm256_loadu_pd((mat->data) + i+4);
            temp = _mm256_andnot_pd(mask, temp);
            _mm256_storeu_pd((result->data) + i+4, temp);
            
            temp = _mm256_loadu_pd((mat->data) + i+8);
            temp = _mm256_andnot_pd(mask, temp);
            _mm256_storeu_pd((result->data) + i+8, temp);
            
            temp = _mm256_loadu_pd((mat->data) + i+12);
            temp = _mm256_andnot_pd(mask, temp);
            _mm256_storeu_pd((result->data) + i+12, temp);
        }
        
        //divisible by 4
        #pragma omp for nowait
        for (int i = 16*(maxi/16); i < 4*(maxi/4); i+=4) {
            temp = _mm256_loadu_pd((mat->data) + i);
            //printf("||%f, %f, %f, %f", (temp)[0], (temp)[1], (temp)[2], (temp)[3]);
            temp = _mm256_andnot_pd(mask, temp);
            _mm256_storeu_pd((result->data) + i, temp);
            //printf("||%f, %f, %f, %f=3=", (result->data)[0], (result->data)[1], (result->data)[2], (result->data)[3]);
        }
        
        #pragma omp for
        for (int i = 4*(maxi/4); i<maxi; i++) {
            (result->data)[i] = fabs((mat->data)[i]);
        }
	}
    return 0;
}

/*
 * Store the result of element-wise negating mat's entries to `result`.
 * Return 0 upon success.
 * Matrix is in row-major order.
 */
int neg_matrix(matrix *result, matrix *mat) {
    int maxi = (mat->cols) * (mat->rows);
    for (int i = 0; i<maxi; i++) {
        (result->data)[i] = -(mat->data)[i];
    }
    return 0;
}

/*
 * Store the result of adding mat1 and mat2 to `result`.
 * Return 0 upon success.
 * `mat1` and `mat2` have the same dimensions.
 * Matrix is in row-major order.
 */
int add_matrix(matrix *result, matrix *mat1, matrix *mat2) {
    int maxi = (mat1->cols) * (mat1->rows);
	
	__m256d temp1;
        __m256d temp2;
        __m256d tempf;
    
    #pragma omp parallel
    {
        #pragma omp for nowait
        for (int i = 0; i < 16*(maxi/16); i+=16) {
            temp1 = _mm256_loadu_pd((mat1->data) + i);
            temp2 = _mm256_loadu_pd((mat2->data) + i);
            tempf = _mm256_add_pd(temp1, temp2);
            _mm256_storeu_pd((result->data) + i, tempf);
            
            temp1 = _mm256_loadu_pd((mat1->data) + i + 4);
            temp2 = _mm256_loadu_pd((mat2->data) + i + 4);
            tempf = _mm256_add_pd(temp1, temp2);
            _mm256_storeu_pd((result->data) + i + 4, tempf);
            
            temp1 = _mm256_loadu_pd((mat1->data) + i + 8);
            temp2 = _mm256_loadu_pd((mat2->data) + i + 8);
            tempf = _mm256_add_pd(temp1, temp2);
            _mm256_storeu_pd((result->data) + i + 8, tempf);
            
            temp1 = _mm256_loadu_pd((mat1->data) + i + 12);
            temp2 = _mm256_loadu_pd((mat2->data) + i + 12);
            tempf = _mm256_add_pd(temp1, temp2);
            _mm256_storeu_pd((result->data) + i + 12, tempf);
        }
        
        //divisible by 4
        #pragma omp for nowait
        for (int i = 16*(maxi/16); i < 4*(maxi/4); i+=4) {
            temp1 = _mm256_loadu_pd((mat1->data) + i);
            temp2 = _mm256_loadu_pd((mat2->data) + i);
            tempf = _mm256_add_pd(temp1, temp2);
            _mm256_storeu_pd((result->data) + i, tempf);
        }
        
        #pragma omp for
        for (int i = 4*(maxi/4); i<maxi; i++) {
            (result->data)[i] = (mat1->data)[i] + (mat2->data)[i];
        }
	}
    return 0;
}

/*
 * Store the result of subtracting mat2 from mat1 to `result`.
 * Return 0 upon success.
 * `mat1` and `mat2` have the same dimensions.
 * Matrix is in row-major order.
 */
int sub_matrix(matrix *result, matrix *mat1, matrix *mat2) {
    int maxi = (mat1->cols) * (mat1->rows);
    for (int i = 0; i<maxi; i++) {
        (result->data)[i] = (mat1->data)[i] - (mat2->data)[i];
    }
    return 0;
}

/*
 * Store the result of multiplying mat1 and mat2 to `result`.
 * Return 0 upon success.
 * Remember that matrix multiplication is not the same as multiplying individual elements.
 * `mat1`'s number of columns is equal to `mat2`'s number of rows.
 */
int mul_matrix(matrix *result, matrix *mat1, matrix *mat2) {
    //Basically mat1(row1,col1) * mat2(col1,col2)
    
    double* tempRow;
    double* tempCol;
    
    //Or (mat1->cols)
    int els = (mat2->rows);
    int tranR = (mat2->cols);
    
    //alternatively use double**
    double* transposed = malloc(els * tranR * sizeof(double));
    if (transposed == NULL) {
        return -1;
    }
    
    __m256d temp0;
    __m256d temp1;
    double temp2[4];

	int i, j, k;
    
    //Go through each col2 or row of transposed
    //Can utilize blocking?
	#pragma omp parallel for private(i, j, tempRow, tempCol, temp0)
    for (int i = 0; i < tranR; i++) {
        tempRow = transposed + els*i;
        tempCol = (mat2->data) + i;
        
        //Go through elements of each column2
        for (int j = 0; j < (els/4)*4; j += 4) {
            //access values from each row
            temp0 = _mm256_set_pd(tempCol[tranR*(j+3)], tempCol[tranR*(j+2)], tempCol[tranR*(j+1)], tempCol[tranR*j]);
            
            _mm256_storeu_pd(tempRow + j, temp0);
        }
        
        for (int j = (els/4)*4; j < els; j++) {
            tempRow[j] = tempCol[tranR*j];
        }
    }
    
    //int index = 0;
    __m256d sum;
    
    //Each row of first matrix
    #pragma omp parallel for private(temp2, tempCol, tempRow, temp1, temp0, sum, i, j, k)
    for (i = 0; i < (mat1->rows); i++) {
        //starting index + (#ofElements/row * row#)
        //tempRow = (mat1->data) + els*i;
        for (j = 0; j < tranR; j++) {
            //double temp2[4];
            
            tempCol = transposed + els*j;
            tempRow = (mat1->data) + els*i;
            
            //multiply row1 by tranRow2 and add together
            sum = _mm256_set1_pd(0);

            for (int k = 0; k < (els/16)*16; k+=16) {
                temp0 = _mm256_loadu_pd(tempRow + k);
                temp1 = _mm256_loadu_pd(tempCol + k);
                sum = _mm256_fmadd_pd(temp0, temp1, sum);
                
                temp0 = _mm256_loadu_pd(tempRow + k+4);
                temp1 = _mm256_loadu_pd(tempCol + k+4);
                sum = _mm256_fmadd_pd(temp0, temp1, sum);

                temp0 = _mm256_loadu_pd(tempRow + k+8);
                temp1 = _mm256_loadu_pd(tempCol + k+8);
                sum = _mm256_fmadd_pd(temp0, temp1, sum);

                temp0 = _mm256_loadu_pd(tempRow + k+12);
                temp1 = _mm256_loadu_pd(tempCol + k+12);
                sum = _mm256_fmadd_pd(temp0, temp1, sum);
            }

            for (k = (els/16)*16; k < (els/4)*4; k+=4) {
                temp0 = _mm256_loadu_pd(tempRow + k);
                temp1 = _mm256_loadu_pd(tempCol + k);
                sum = _mm256_fmadd_pd(temp0, temp1, sum);
            }
            
            _mm256_storeu_pd(temp2, sum);
            
            for (int k = (els/4)*4; k < els; k++) {
                temp2[k%4] += tempRow[k] * tempCol[k];
            }
	    
            (result->data)[i * tranR + j] = temp2[0] + temp2[1] + temp2[2] + temp2[3];
        }
    }
	//printf("||%f, %f, %f, %f===", (result->data)[0], (result->data)[1], (result->data)[2], (result->data)[3]);
    
    free(transposed);
    return 0;
}

/*
 * Store the result of raising mat to the (pow)th power to `result`.
 * Return 0 upon success.
 * Pow is matrix multiplication, not element-wise multiplication.
 * `mat` is a square matrix and `pow` is a non-negative integer.
 * Matrix is in row-major order.
 */
int pow_matrix(matrix *result, matrix *mat, int pow) {
    if (pow == 0) {
        fill_matrix(result, 0);
        /* Put the identity matrix in the result matrix */
        #pragma omp parallel for
        for (int i = 0; i < (result->cols); i++) {
            (result->data)[i + i*(result->cols)] = 1;
        }
    }
    else if (pow == 1) {
        /* Put the values of the original matrix into the result matrix */
        //*result = *mat;
        memcpy(result->data, mat->data, (mat->cols)* (mat->rows)  * sizeof(double));
    }
    else {
        int maxi = (mat->cols) * (mat->rows) * sizeof(double);
        
        matrix* temp = (matrix*)malloc(sizeof(matrix));
        temp->data = (double*)malloc(maxi);
        temp->cols = mat->cols;
        temp->rows = mat->rows;
        
        //store data of x^2 into temp
        mul_matrix(temp, mat, mat);
        
        //"Exponentiation by squaring" algorithm
        //if odd
        if (pow%2 == 1) {
            //x * (x^2)^((n-1)/2)
            pow_matrix(result, temp, (pow-1)/2);
            mul_matrix(result, mat, result);
        }
	    else {
            pow_matrix(result, temp, pow/2);
		}
        
        free(temp->data);
        free(temp);
    }
    return 0;
}
