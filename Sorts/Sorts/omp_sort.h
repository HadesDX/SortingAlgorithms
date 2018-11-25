#ifndef OMP_SORT_PP_CLASS_H
#define OMP_SORT_PP_CLASS_H

#include "pch.h"
#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>
#include <omp.h>
#include <math.h>

#define MAX(A, B) (((A) > (B)) ? (A) : (B))
#define MIN(A, B) (((A) > (B)) ? (B) : (A))
#define UP 0
#define DOWN 1
#define VERBOSE 0

int omp_bubble_sort(int *A, int n);
int omp_odd_even_sort(int *A, int n);
int omp_rank_sort(int *A, int n);
int omp_counting_sort(int *x, int n);
int swap(int *a, int *b);
int bitonic_sort_seq(int start, int length, int *A, int flag);
int bitonic_sort_par(int start, int length, int *A, int flag, int m);
int bitonic(int *A, int n);
static int CmpInt(const void *a, const void *b);
void merge(int A[], int B[], int m, int n);
void arraymerge(int *a, int size, int *index, int N);
int QuickSort(int *a, int size);
int RadixSort(int *input, int n);
void Mergesort(int *x, int a, int b);
void mix_them(int *x, int n, int h, int num_threads);
void mix(int *x, int a1, int b1, int a2, int b2);
int Mergesort_Omp(int *x, int n);

// Bubble sort -------------------------------------------------------------------
int omp_bubble_sort(int *A, int n) {
	int n_pros = omp_get_num_threads();
	int chunk = n / n_pros, bandera = 1, nr = 0, i, j;
	int aux;
	while (bandera) {
		nr++;
		bandera = 0;
#pragma omp parallel for reduction(+:bandera) private(aux)
		for (j = 0; j < n_pros; j++) {
			for (i = j; i < n - 1; i = i + n_pros) {
				if (A[i] > A[i + 1]) {
					aux = A[i];
					A[i] = A[i + 1];
					A[i + 1] = aux;
					++bandera;
				}
			}
		}
	}
	return 0;
}
// end bubble sort -------------------------------------------------------------------

// odd even transposition sort -------------------------------------------------------------------
int omp_odd_even_sort(int *A, int n) {

	int sorted, i;

	sorted = 1;

	while (sorted != 0) {

#pragma omp parallel
		{

			sorted = 0;
#pragma omp for reduction(+:sorted)
			for (i = 0; i < n - 1; i += 2) {
				if (A[i] > A[i + 1]) {
					int temp = A[i];
					A[i] = A[i + 1];
					A[i + 1] = temp;
					sorted++;
				}
			}

#pragma omp for reduction(+:sorted)
			for (i = 1; i < n - 1; i += 2) {
				if (A[i] > A[i + 1]) {
					int temp = A[i];
					A[i] = A[i + 1];
					A[i + 1] = temp;
					sorted++;
				}
			}

		}
	}

	return 0;

}
// end odd even transposition sort -------------------------------------------------------------------

// rank sort -------------------------------------------------------------------
int omp_rank_sort(int *A, int n) {
	int *y;

	y = (int *)calloc(n, sizeof(int));

#pragma omp parallel
	{
		int threads = omp_get_num_threads();
		int rank, i, j, startval, endval, my_num, my_place;

		rank = omp_get_thread_num();
		startval = n * rank / threads;
		endval = n * (rank + 1) / threads;

		//printf("   %d %d\n",startval, endval);

		for (j = startval; j < endval; j++) {
			my_num = A[j];
			my_place = 0;
			for (i = 0; i < n; i++) {
				if (my_num > A[i])
					my_place++;
			}

			y[my_place] = my_num;

		}

	}

	int k;
	for (k = 0; k < n; k++) {
		if (y[k] == 0)
			A[k] = A[k - 1];
		else
			A[k] = y[k];
	}

	free(y);

	return 0;

}
//end rank sort -------------------------------------------------------------------

// counting sort -------------------------------------------------------------------
int omp_counting_sort(int *A, int n) {
	//Enteros en [1,m] y variables
	int m = 1001, i;
	int *c = (int*)calloc(m, sizeof(int));
	int *b = (int*)calloc(n, sizeof(int));

	//printf("1\n");

	//Copiando x en b ( solo aki se paraleliza XD )
#pragma omp parallel for private(i) shared(A,b)
	for (i = 0; i < n; i++) {
		b[i] = A[i];
	}

	//printf("2\n");

	//Contando
	for (i = 0; i < n; i++) {
		//printf("%d ", b[i]);
		c[b[i] - 1]++;				//TODO index error -1
	}

	//printf("3\n");

	//Suma prefija
	for (i = 1; i < m; i++)
		c[i] += c[i - 1];

	//printf("4\n");

	//Ordenando
	for (i = 0; i < n; i++) {
		A[c[b[i] - 1] - 1] = b[i];
		c[b[i] - 1]--;
	}

	//printf("5\n");

	//Liberando arreglos auxiliares
	free(c);
	free(b);

	return 0;
}
// end counting sort -------------------------------------------------------------------


// bitonic sort -------------------------------------------------------------------
int swap(int *a, int *b) {
	int temp;
	temp = *a;
	*a = *b;
	*b = temp;
	return 0;
}
int bitonic_sort_seq(int start, int length, int *A, int flag) {
	int i;
	int split_length;
	if (length == 1)
		return 1;
	if (length % 2 != 0) {
		printf("error\n");
		exit(0);
	}
	split_length = length / 2;
	// bitonic split
	for (i = start; i < start + split_length; i++) {
		if (flag == UP) {
			if (A[i] > A[i + split_length])
				swap(&A[i], &A[i + split_length]);
		} else {
			if (A[i] < A[i + split_length])
				swap(&A[i], &A[i + split_length]);
		}
	}
	bitonic_sort_seq(start, split_length, A, flag);
	bitonic_sort_seq(start + split_length, split_length, A, flag);
	return 0;
}


int bitonic_sort_par(int start, int length, int *A, int flag, int m) {
	int i;
	int split_length;
	if (length == 1) {
		return 0;
	}
	// la longitud de la subsecuencia debe ser potencia de 2
	if (length % 2 != 0) {
		exit(0);
	}
	split_length = length / 2;
	// bitonic split
#pragma omp parallel for shared(A, flag, start, split_length) private(i)
	for (i = start; i < start + split_length; i++) {
		if (flag == UP) {
			if (A[i] > A[i + split_length])
				swap(&A[i], &A[i + split_length]);
		} else {
			if (A[i] < A[i + split_length])
				swap(&A[i], &A[i + split_length]);
		}
	}
	if (split_length > m) {
		bitonic_sort_par(start, split_length, A, flag, m);
		bitonic_sort_par(start + split_length, split_length, A, flag, m);
	}
	return 0;
}
/**	\brief Bitonic Sort. Algoritmo de ordenamiento para conjuntos de datos cuya longitus es potencia de 2.
*/
int bitonic(int *A, int n) {
	int m;
	int i, j;
	int flag;
	int num_threads;
	//Numero de hilos con los que se esta trabajando
	num_threads = omp_get_num_threads();
	//n debe ser almenos mayor que el doble de procesadores
	if (n < num_threads * 2) {
		return 1;
	}
	// particionando
	m = n / num_threads;
	//Primer parte del bitonic donde particionamos
	for (i = 2; i <= m; i = 2 * i) {
#pragma omp parallel for shared(i, A) private(j, flag)
		for (j = 0; j < n; j += i) {
			if ((j / i) % 2 == 0)
				flag = UP;
			else
				flag = DOWN;
			bitonic_sort_seq(j, i, A, flag);
		}
	}
	// Segunda parte del bitonic
	for (i = 2; i <= num_threads; i = 2 * i) {
		for (j = 0; j < num_threads; j += i) {
			if ((j / i) % 2 == 0)
				flag = UP;
			else
				flag = DOWN;
			bitonic_sort_par(j*m, i*m, A, flag, m);
		}
#pragma omp parallel for shared(j)
		for (j = 0; j < num_threads; j++) {
			if (j < i)
				flag = UP;
			else
				flag = DOWN;
			bitonic_sort_seq(j*m, m, A, flag);
		}
	}
	return 0;
}
// end bitonic sort -------------------------------------------------------------------

// quick sort -------------------------------------------------------------------

static int CmpInt(const void *a, const void *b) {
	return (*(int*)a - *(int*)b);
}

/* Merge sorted lists A and B into list A.  A must have dim >= m+n */
void merge(int A[], int B[], int m, int n) {
	int i = 0, j = 0, k = 0, p;
	int size = m + n;
	int *C = (int *)malloc(size * sizeof(int));
	while (i < m && j < n) {
		if (A[i] <= B[j]) C[k] = A[i++];
		else C[k] = B[j++];
		k++;
	}
	if (i < m) for (p = i; p < m; p++, k++) C[k] = A[p];
	else for (p = j; p < n; p++, k++) C[k] = B[p];
	for (i = 0; i < size; i++) A[i] = C[i];
	free(C);
}

/* Merges N sorted sub-sections of array a into final, fully sorted array a */
void arraymerge(int *a, int size, int *index, int N) {
	int i, j;
	while (N > 1) {
		for (i = 0; i < N; i++) index[i] = i * size / N; index[N] = size;
#pragma omp parallel for private(i)
		for (i = 0; i < N; i += 2) {
			if (VERBOSE) fprintf(stderr, "merging %d and %d, index %d and %d (up to %d)\n", i, i + 1, index[i], index[i + 1], index[i + 2]);
			merge(a + index[i], a + index[i + 1], index[i + 1] - index[i], index[i + 2] - index[i + 1]);
			if (VERBOSE) for (j = 0; j < size; j++) fprintf(stderr, "after: %d %d\n", j, a[j]);
		}
		N /= 2;
	}
}


int QuickSort(int *a, int size) {
	// set up threads
	int i = 0;
	int threads = omp_get_max_threads();
	omp_set_num_threads(threads);
	int *index = (int *)malloc((threads + 1) * sizeof(int));
	for (i = 0; i < threads; i++) index[i] = i * size / threads; index[threads] = size;

	/* Main parallel sort loop */
#pragma omp parallel for private(i)
	for (i = 0; i < threads; i++) qsort(a + index[i], index[i + 1] - index[i], sizeof(int), CmpInt);
	/* Merge sorted array pieces */
	if (threads > 1) arraymerge(a, size, index, threads);

	return 0;
}


// end quick sort -------------------------------------------------------------------


// radix sort
int RadixSort(int *input, int n) {

	int d = 1001; //highest-order digit
	int b = 10, k = d;////
	int *temp = (int*)malloc(k * sizeof(int)); //used in counting sort
	int *input2 = (int*)malloc(sizeof(int)*n);
	int *output = (int*)malloc(sizeof(int)*n);
	int power = ((int)pow(2, b) - 1);

	int l;

	int t = omp_get_num_threads(); //UNUSED
	//omp_set_num_threads(t);

	//main loop
	for (l = 0; l < 32 / b; l++) {
		int slice = l * b;
		int i, j;

		if (slice <= d) {
#pragma omp parallel
			{
#pragma omp for schedule(guided) private(i)
				for (i = 0; i < n; i++)
					input2[i] = (input[i] >> slice) & power;
#pragma omp for schedule(guided) private(i)
				for (i = 0; i < k; i++)
					temp[i] = 0;
#pragma omp for schedule(guided) private(j)
				for (j = 0; j < n; j++)
#pragma omp atomic
					temp[input2[j]]++;
#pragma omp for ordered schedule(guided) private(i)
				for (i = 1; i < k; i++)
#pragma omp ordered
					temp[i] += temp[i - 1];
			}

			for (j = n - 1; j >= 0; j--) {
				output[temp[input2[j]] - 1] = input[j];
				temp[input2[j]]--;
			}
#pragma omp parallel for schedule(guided) private(j)
			for (j = 0; j < n; j++)
				input[j] = output[j];
		}
	}
	return 0;
}

//end radix sort -------------------------------------------------------------------


//merge sort -------------------------------------------------------------------

//Funcion para mezclar 2 subvectores ordenados contiguos
void mix(int *x, int a1, int b1, int a2, int b2) {
	//Obteniendo numero de eltos en cada subvector
	int n1 = b1 - a1 + 1;
	int n2 = b2 - a2 + 1;

	//Creando copias de subvectores
	int *x1 = (int*)calloc(n1, sizeof(int));
	int *x2 = (int*)calloc(n2, sizeof(int));

	//Copiando subvectores
	int i, i1 = 0, i2 = 0;
	for (i = 0; i < n1; i++)
		x1[i] = x[a1 + i];
	for (i = 0; i < n2; i++)
		x2[i] = x[a2 + i];

	//Limpiando iterador
	i = a1;

	//Iterando sobre el rango de ambos subvectores
	while (i1 < n1 && i2 < n2) {
		//Determinando el elto menor, copiandolo al
		//buffer y recorriendo su apuntador
		if (x1[i1] < x2[i2]) {
			x[i] = x1[i1];
			i1++;
		} else {
			x[i] = x2[i2];
			i2++;
		}

		//Incrementando indice de x
		i++;

	}//Fin de while sobre ambos subvectores

	//Terminando el subvector 1 si aun no termina
	while (i1 < n1) {
		//Actualizando valor y recorriendo indices
		x[i] = x1[i1];
		i++;
		i1++;
	}

	//Terminando el subvector 2 si aun no termina
	while (i2 < n2) {
		//Actualizando valor y recorriendo indices
		x[i] = x2[i2];
		i++;
		i2++;
	}

	//Liberando memoria
	free(x1);
	free(x2);

}

//Mergesort de un subvector en los indices [a,b]
void Mergesort(int *x, int a, int b) {
	//Indices de subvectores
	int a1, a2, b1, b2;

	//Numero de eltos
	int n = b - a + 1;

	//Evitando el caso trivial
	if (n > 1) {
		a1 = a;
		b1 = (int)(a1 + floor(1.0*n / 2) - 1);
		a2 = b1 + 1;
		b2 = b;

		//Ordenando subarreglos recursivamente
		Mergesort(x, a1, b1);
		Mergesort(x, a2, b2);

		//Mezclando subarreglos ordenados
		mix(x, a1, b1, a2, b2);

	}//Fin de caso no trivial

}

//Funcion para mezclar los subvectores usados en Mergesort_Omp
//El fin de esta funcion es decidir un orden adecuado para
//mezclar los subvectores ordenados por los threads utilizados
//en la funcion Mergesort_Omp.
//h es el numero de eltos a ordenar en cada thread:
//h = n/num_threads
void mix_them(int *x, int n, int h, int num_threads) {
	//variables de limites de subvectores
	int a1, b1, a2, b2;

	//Actuando acorde al numero de threads
	switch (num_threads) {
		//No mezcla nada
	case 1:
		break;

	case 2:
	{
		//calculando limites
		a1 = 0;       b1 = h - 1;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando
		mix(x, a1, b1, a2, b2);

		//Fin para 2 threads
		break;
	}
	case 3:
	{
		//calculando limites de subvectores 1 y 2
		a1 = 0;       b1 = h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 1 y 2 = subvector @
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores @ y 3
		b1 = b2;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores @ y 3
		mix(x, a1, b1, a2, b2);

		//Fin para 3 threads
		break;
	}
	case 4:
	{
		//calculando limites de subvectores 1 y 2
		a1 = 0;       b1 = h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 1 y 2 = subvector @
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores 3  y 4
		a1 = b2 + 1; b1 = a1 + h - 1;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores 3 y 4 = subvector $
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores @  y $
		b1 = a1 - 1; a1 = 0;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores @ y $
		mix(x, a1, b1, a2, b2);

		//Fin para 4 threads
		break;
	}
	case 5:
	{
		//calculando limites de subvectores 1 y 2
		a1 = 0;       b1 = h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 1 y 2 = subvector @
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores @  y 3
		a1 = 0;       b1 = b2;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores @ y 3 = subvector $
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores 4  y 5
		a1 = b2 + 1; b1 = a1 + h - 1;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores 4 y 5 = subvector %
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores $  y %
		b1 = a1 - 1; a1 = 0;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores $ y %
		mix(x, a1, b1, a2, b2);

		//Fin para 5 threads
		break;
	}
	case 6:
	{
		//calculando limites de subvectores 1 y 2
		a1 = 0;        b1 = h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 1 y 2 = subvector @
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores 3  y 4
		a1 = b2 + 1; b1 = a1 + h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 3 y 4 = subvector $
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores 5  y 6
		a1 = b2 + 1; b1 = a1 + h - 1;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores 5 y 6 = subvector &
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores @  y $
		a1 = 0;       b1 = 2 * h - 1;
		a2 = b1 + 1; b2 = a2 + 2 * h - 1;

		//mezclando subvectores @ y $ = subvector %
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores %  y &
		a1 = 0;        b1 = b2;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores %  y &
		mix(x, a1, b1, a2, b2);

		//Fin para 6 threads
		break;
	}
	case 7:
	{
		//calculando limites de subvectores 1 y 2
		a1 = 0;       b1 = h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 1 y 2 = subvector @
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores 3  y 4
		a1 = b2 + 1; b1 = a1 + h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 3 y 4 = subvector $
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores 5  y 6
		a1 = b2 + 1; b1 = a1 + h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 5 y 6 = subvector &
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores @  y $
		a1 = 0;       b1 = 2 * h - 1;
		a2 = b1 + 1; b2 = a2 + 2 * h - 1;

		//mezclando subvectores @ y $ = subvector %
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores &  y 7
		a1 = b2 + 1; b1 = a1 + 2 * h - 1;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores @ y $ = subvector #
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores %  y #
		a1 = 0;       b1 = 4 * h - 1;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores %  y #
		mix(x, a1, b1, a2, b2);

		//Fin para 7 threads
		break;
	}
	case 8:
	{
		//calculando limites de subvectores 1 y 2
		a1 = 0;       b1 = h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 1 y 2 = subvector @
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores 3  y 4
		a1 = b2 + 1; b1 = a1 + h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 3 y 4 = subvector $
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores 5  y 6
		a1 = b2 + 1; b1 = a1 + h - 1;
		a2 = b1 + 1; b2 = a2 + h - 1;

		//mezclando subvectores 5 y 6 = subvector &
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores 7  y 8
		a1 = b2 + 1; b1 = a1 + h - 1;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores 7 y 8 = subvector #
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores @  y $
		a1 = 0;       b1 = 2 * h - 1;
		a2 = b1 + 1; b2 = a2 + 2 * h - 1;

		//mezclando subvectores @ y $ = subvector %
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores &  y #
		a1 = b2 + 1; b1 = a1 + 2 * h - 1;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores & y # = subvector ~
		mix(x, a1, b1, a2, b2);

		//calculando limites de subvectores %  y ~
		a1 = 0;       b1 = 4 * h - 1;
		a2 = b1 + 1; b2 = n - 1;

		//mezclando subvectores %  y ~
		mix(x, a1, b1, a2, b2);

		//Fin para 8 threads
		break;
	}
	default:
	{

	}
	}
}

//Mergesort con openmp
int Mergesort_Omp(int *x, int n) {
	//Obteniendo numero de threads
	int num_threads = omp_get_max_threads(), i;

	//Obteniendo # de eltos a ordenar por cada thread
	int h = n / num_threads;

	//Indices del arreglo original de inicio y final de cada thread
	int *I_start = (int*)calloc(num_threads, sizeof(int));
	int *I_end = (int*)calloc(num_threads, sizeof(int));

	//Llenando el indice de arreglos. Lo llenamos de esta manera para
	//q el ultimo indice de final sea n-1
	for (i = 0; i < num_threads - 1; i++) {
		I_start[i] = i * h;
		I_end[i] = I_start[i] + h - 1;
	}

	I_start[i] = i * h;
	I_end[i] = n - 1;

	//Ordenando los num_threads subarreglos
#pragma omp parallel for
	for (i = 0; i < num_threads; i++) {
		//Obteniendo numero de thread
		int my_rank = omp_get_thread_num();

		//Ordenando su segmento
		Mergesort(x, I_start[my_rank], I_end[my_rank]);
	}

	//Ordenando los subarreglos entre si
	mix_them(x, n, h, num_threads);

	//Liberando memoria
	free(I_start);
	free(I_end);

	return 0;

}

//end merge sort -------------------------------------------------------------------

int N_OMP_SORT_FUNCS = 8;
typedef int(*omp_f) (int*, int);
omp_f omp_funcs[] = { &omp_bubble_sort, &omp_odd_even_sort, &QuickSort, &RadixSort, &Mergesort_Omp,&bitonic, &omp_rank_sort, &omp_counting_sort };
const char *omp_funcs_names[] = { "omp_bubble_sort", "omp_odd_even_sort", "QuickSort", "RadixSort", "Mergesort_Omp", "bitonic", "omp_rank_sort", "omp_counting_sort" };


#endif
