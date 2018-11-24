#include "pch.h"
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "sys/time.h"
#include <malloc.h>
#include <omp.h>
#include <string.h>
//#include <LibC_Project/Sort/omp_sort.h>
//#include "C:\Program Files (x86)\CodeBlocks\MinGW\include\LibC_Project\Sort\omp_sort.h"
#include "omp_sort.h"

extern omp_f omp_funcs[];
extern const char* omp_funcs_names[];
extern int N_OMP_SORT_FUNCS;
/** \brief Funci&oacute;n b&aacute;sica para verificar si un conjunto
esta o no ordenado.

*/
int is_sorted(int *A, int n) {
	int i;
	for (i = 0; i < n - 1; ++i) {
		if (A[i] > A[i + 1]) {
			return 0;
		}
	}
	return 1;
}

/** \brief Funci&oacute;n auxiliar que crea un sufijo para mis
	cadenas de caracteres.
	\param aux_num Variable auxiliar donde guardaremos la cadena.
	\param n_max N&uacute;mero de d&iacute;gitos que tendra el sufijo.
	\param n El n&uacute;mero a ser convertido en cadena.
	\todo El limite de corrida es 999,999.
*/
int genera_indice(char *aux_num, int n_max, int n, int corrida) {
	int i;
	int m1, m2;
	char aux2[50];
	char aux3[50];
	aux_num[0] = '\0';

	sprintf_s(aux2, "%d", n);
	m1 = (int)strlen(aux2);
	for (i = 0; i < n_max - m1; i++) {
		strcat(aux_num, "0");
	}
	strcat(aux_num, aux2);
	if (corrida > 0) {
		sprintf(aux3, "%d", corrida);
		m2 = (int)strlen(aux3);
		strcat(aux_num, "_");
		for (i = 0; i < 6 - m2; i++) {
			strcat(aux_num, "0");
		}
		strcat(aux_num, aux3);
	}
	return 0;
}

/** \brief genera_lista_enteros


*/
int genera_lista_enteros(char *file_path_out, int N, int a, int b) {
	int i, n_aleatorio;
	char file_name_out[250];
	char aux[50];
	FILE *out;
	strcpy(file_name_out, file_path_out);
	genera_indice(aux, 7, N, 0);
	strcat(file_name_out, aux);
	strcat(file_name_out, "_");
	genera_indice(aux, 7, a, 0);
	strcat(file_name_out, aux);
	strcat(file_name_out, "_");
	genera_indice(aux, 7, b, 0);
	strcat(file_name_out, aux);
	strcat(file_name_out, ".in");
	out = fopen(file_name_out, "w");
	if (out != NULL) {
		fprintf(out, "%d\n", N);
		for (i = 0; i < N; ++i) {
			n_aleatorio = a + rand() % (b - a);
			fprintf(out, "%d\n", n_aleatorio);
		}
		fclose(out);
	}
	return 0;
}

int main(int argc, char *argv[]) {
	int N, N_samples, N_THREADS, N_PROCESADORES, i, j, k, n_funciones, leyo;
	double secs, **TE, *TPROM, *TMIN, *TMAX, *TVAR;
	struct timeval t_start, t_end;
	char file_name_in[250], file_name_out[250], file_path_out[250], file_path_in[250], file_name_log[250];
	char aux_machine_index[50];
	FILE *in, *out, *log_file;
	strcpy(aux_machine_index, "_29");
	if (argc == 4) {
		strcpy(file_path_out, "../resultados/");
		strcpy(file_path_in, "../data_in/");
		strcpy(file_name_in, argv[1]);
		strcpy(file_name_in, file_path_in);
		strcpy(file_name_out, file_path_out);
		strcpy(file_name_log, file_path_out);
		strcat(file_name_log, "log.txt");
		N_samples = atoi(argv[2]);
		N_THREADS = atoi(argv[3]);
		omp_set_num_threads(N_THREADS);
		n_funciones = 5;
		omp_set_num_threads(N_THREADS);
		N_PROCESADORES = omp_get_num_procs();
		//omp_set_num_threads(N_THREADS);
		in = fopen(file_name_in, "r");
		if (in != NULL) {
			leyo = fscanf(in, "%d", &N);
			int *data = (int*)malloc(sizeof(int)*N);
			int *resp_data = (int*)malloc(sizeof(int)*N);
			TE = (double **)malloc(sizeof(double *)*n_funciones);
			for (i = 0; i < n_funciones; ++i) {
				TE[i] = (double *)malloc(sizeof(double)*N);
			}
			TPROM = (double *)malloc(sizeof(double)*n_funciones);
			TMIN = (double *)malloc(sizeof(double)*n_funciones);
			TMAX = (double *)malloc(sizeof(double)*n_funciones);
			TVAR = (double *)malloc(sizeof(double)*n_funciones);
			for (i = 0; i < n_funciones; ++i) {
				TPROM[i] = 0.0;
				TMIN[i] = 0.0;
				TMAX[i] = 0.0;
				TVAR[i] = 0.0;
			}
			for (i = 0; i < N; ++i) {
				leyo = fscanf(in, "%d", &data[i]);
				resp_data[i] = data[i];
			}
			for (i = 0; i < n_funciones; ++i) {
				for (j = 0; j < N_samples; ++j) {
					gettimeofday(&t_start, 0);
					omp_funcs[i](data, N);
					gettimeofday(&t_end, 0);
					if (!is_sorted(data, N)) {
						printf("Algo salio mal!!!!");
						log_file = fopen(file_name_log, "w");
						if (log_file != NULL) {
							fprintf(log_file, "Error en algoritmo %s", omp_funcs_names[i]);
							fclose(log_file);
						}
					}
					secs = t_end.tv_sec - t_start.tv_sec + (t_end.tv_usec - t_start.tv_usec)*1e-6;
					TE[i][j] = secs;
					for (k = 0; k < N; ++k) {
						data[i] = resp_data[i];
					}
				}
			}
			free(resp_data);
			free(data);
			for (i = 0; i < n_funciones; ++i) {
				for (j = 0; j < N; ++j) {
					TPROM[i] += TE[i][j];
				}
				TPROM[i] /= (double)N;
				TMIN[i] = TPROM[i];
				TMAX[i] = TPROM[i];
			}
			for (i = 0; i < n_funciones; ++i) {
				for (j = 0; j < N; ++j) {
					if (TMIN[i] > TE[i][j]) {
						TMIN[i] = TE[i][j];
					}
					if (TMAX[i] < TE[i][j]) {
						TMAX[i] = TE[i][j];
					}
					TVAR[i] += (TPROM[i] - TE[i][j])*(TPROM[i] - TE[i][j]);
				}
				TVAR[i] /= (double)N;
			}
			out = fopen(file_name_out, "w");
			if (out != NULL) {
				fprintf(out, "\\captionof{table}{Maquina %s \\\\Numero de procesadores disponibles %d \\\\Numero de threads %d \\Numero Iteraciones}%% \n", aux_machine_index, N_PROCESADORES, N_THREADS, N_samples);
				//printf("Maquina %s \nNumero de procesadores disponibles %d \nNumero de threads %d\n",aux_machine_index,N_PROCESADORES,N_THREADS);
				fprintf(out, "$$\\begin{tabular}{|c|c|c|c|c|} \n");
				fprintf(out, "\\hline \n");
				fprintf(out, "Metodo & $\\mu$ & MIN & MAX & $\\sigma^{2}$\\\\ \n");
				fprintf(out, "\\hline \n");
				for (i = 0; i < n_funciones; i++) {
					fprintf(out, "%s & %.3E & %.3E & %.3E & %.3E\\\\ \n", omp_funcs_names[i], TPROM[i], TMIN[i], TMAX[i], TVAR[i]);
					//printf("Experimento %d %E\n",i+1,TE[i]);
					fprintf(out, "\\hline \n");
				}
				fprintf(out, "\\end{tabular}$$");
				fclose(out);
			}
			free(TVAR);
			free(TMAX);
			free(TMIN);
			free(TPROM);
			for (i = 0; i < n_funciones; ++i) {
				free(TE[i]);
			}
			free(TE);
		} else {
			printf("Error al intentar abrir %s\n", file_name_in);
		}
	} else {
		printf("NO ARGS:\n");
		//strcpy(file_path_out,"C:\\Users\\Pepe\\Documents\\CIMAT\\3 Semestre\\practicas_sort\\sort_int\\");
		//strcpy(file_path_in,"C:\\Users\\Pepe\\Documents\\CIMAT\\3 Semestre\\practicas_sort\\sort_int\\");
		strcpy(file_path_out, "../resultados/");
		strcpy(file_path_in, "../data_in/");
		//genera_lista_enteros(file_path_out,1000000,0,10000);
		//genera_lista_enteros(file_path_in, 100000,0,1000);
		//genera_lista_enteros(file_path_in, 200000,0,1000);
		//genera_lista_enteros(file_path_in, 300000,0,1000);
		//genera_lista_enteros(file_path_in, 400000,0,1000);
		//genera_lista_enteros(file_path_in, 500000,0,1000);
		//genera_lista_enteros(file_path_in, 600000,0,1000);
		//genera_lista_enteros(file_path_in, 700000,0,1000);
		//genera_lista_enteros(file_path_in, 800000,0,1000);
		//genera_lista_enteros(file_path_in, 900000,0,1000);
		//genera_lista_enteros(file_path_in,1000000,0,1000);
		//genera_lista_enteros(file_path_in,2000000,0,1000);
		//genera_lista_enteros(file_path_in,3000000,0,1000);
		//genera_lista_enteros(file_path_in,4000000,0,1000);
		//genera_lista_enteros(file_path_in,5000000,0,1000);
		//genera_lista_enteros(file_path_in,6000000,0,1000);
		//genera_lista_enteros(file_path_in,7000000,0,1000);
		//genera_lista_enteros(file_path_in,8000000,0,1000);
		//genera_lista_enteros(file_path_in,9000000,0,1000);
		//genera_lista_enteros(file_path_out,10000,0,1000);
		//genera_lista_enteros(file_path_out,1000,0,1000);
		strcpy(file_name_in, file_path_in);
		strcpy(file_name_out, file_path_out);
		strcpy(file_name_log, file_path_out);
		strcat(file_name_log, "log.txt");
		strcat(file_name_in, "0100000_0000000_0001000.in");
		strcat(file_name_out, "0100000_0000000_0001000.out");
		//strcat(file_name_in, "0200000_0000000_0001000.in");
		//strcat(file_name_out, "0200000_0000000_0001000.out");
		//strcat(file_name_in, "0500000_0000000_0001000.in");
		//strcat(file_name_out, "0500000_0000000_0001000.out");
		//strcat(file_name_in, "9000000_0000000_0001000.in");
		//strcat(file_name_out, "9000000_0000000_0001000.out");
		//N_samples = 100;
		//N_THREADS = 8;
		//n_funciones = 5;
		N_samples = 1;
		N_THREADS = 4;
		n_funciones = 8;
		omp_set_num_threads(N_THREADS);
		N_PROCESADORES = omp_get_num_procs();
		//omp_set_num_threads(N_THREADS);
		in = fopen(file_name_in, "r");
		if (in != NULL) {
			leyo = fscanf(in, "%d", &N);
			int *data = (int*)malloc(sizeof(int)*N);
			int *resp_data = (int*)malloc(sizeof(int)*N);
			TE = (double **)malloc(sizeof(double *)*n_funciones);
			for (i = 0; i < n_funciones; ++i) {
				TE[i] = (double *)malloc(sizeof(double)*N);
			}
			TPROM = (double *)malloc(sizeof(double)*n_funciones);
			TMIN = (double *)malloc(sizeof(double)*n_funciones);
			TMAX = (double *)malloc(sizeof(double)*n_funciones);
			TVAR = (double *)malloc(sizeof(double)*n_funciones);
			for (i = 0; i < n_funciones; ++i) {
				TPROM[i] = 0.0;
				TMIN[i] = 0.0;
				TMAX[i] = 0.0;
				TVAR[i] = 0.0;
			}
			for (i = 0; i < N; ++i) {
				leyo = fscanf(in, "%d", &data[i]);
				resp_data[i] = data[i];
			}
			printf("N: %d\n", N);
			for (i = 0; i < n_funciones; ++i) {
				printf("Running function: %d - %s\n", i, omp_funcs_names[i]);
				for (j = 0; j < N_samples; ++j) {
					printf("\tRunning sample: %d\n", j);
					gettimeofday(&t_start, 0);
					omp_funcs[i](data, N);
					gettimeofday(&t_end, 0);
					printf("\t\tEnd sample: %d\n", j);
					printf("\t\tVeryfing sample: %d\n", j);
					if (!is_sorted(data, N)) {
						printf("\t\t\tAlgo salio mal!!!!\n");
						log_file = fopen(file_name_log, "a");
						if (log_file != NULL) {
							fprintf(log_file, "Error en algoritmo %s\n", omp_funcs_names[i]);
							fclose(log_file);
						}
					} else {
						printf("\t\tOK\n");
						log_file = fopen(file_name_log, "a");
						if (log_file != NULL) {
							fprintf(log_file, "OK Sorted %s\n", omp_funcs_names[i]);
							fclose(log_file);
						}
					}

					secs = t_end.tv_sec - t_start.tv_sec + (t_end.tv_usec - t_start.tv_usec)*1e-6;
					printf("\t\tTime: %f\n", secs);
					TE[i][j] = secs;
					for (k = 0; k < N; ++k) {
						data[k] = resp_data[k];
					}
				}
				printf("End Running function: %d\n", i);
			}
			free(resp_data);
			free(data);
			for (i = 0; i < n_funciones; ++i) {
				for (j = 0; j < N; ++j) {
					TPROM[i] += TE[i][j];
				}
				TPROM[i] /= (double)N;
				TMIN[i] = TPROM[i];
				TMAX[i] = TPROM[i];
			}
			for (i = 0; i < n_funciones; ++i) {
				for (j = 0; j < N; ++j) {
					if (TMIN[i] > TE[i][j]) {
						TMIN[i] = TE[i][j];
					}
					if (TMAX[i] < TE[i][j]) {
						TMAX[i] = TE[i][j];
					}
					TVAR[i] += (TPROM[i] - TE[i][j])*(TPROM[i] - TE[i][j]);
				}
				TVAR[i] /= (double)N;
			}
			out = fopen(file_name_out, "a");
			if (out != NULL) {
				fprintf(out, "\\captionof{table}{Maquina %s \\\\Numero de procesadores disponibles %d \\\\Numero de threads %d}%% \n", aux_machine_index, N_PROCESADORES, N_THREADS);
				//printf("Maquina %s \nNumero de procesadores disponibles %d \nNumero de threads %d\n",aux_machine_index,N_PROCESADORES,N_THREADS);
				fprintf(out, "$$\\begin{tabular}{|c|c|c|c|c|} \n");
				fprintf(out, "\\hline \n");
				fprintf(out, "Metodo & $\\mu$ & MIN & MAX & $\\sigma^{2}$\\\\ \n");
				fprintf(out, "\\hline \n");
				for (i = 0; i < n_funciones; i++) {
					fprintf(out, "%s & %.3E & %.3E & %.3E & %.3E\\\\ \n", omp_funcs_names[i], TPROM[i], TMIN[i], TMAX[i], TVAR[i]);
					//printf("Experimento %d %E\n",i+1,TE[i]);
					fprintf(out, "\\hline \n");
				}
				fprintf(out, "\\end{tabular}$$");
				fclose(out);
			}
			free(TVAR);
			free(TMAX);
			free(TMIN);
			free(TPROM);
			for (i = 0; i < n_funciones; ++i) {
				free(TE[i]);
			}
			free(TE);
		} else {
			printf("Error al intentar abrir %s\n", file_name_in);
		}
	}
	return 0;
}

