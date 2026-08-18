package br.mytracker.mstask.service;

import br.mytracker.mstask.domain.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TaskService {

    private final ConcurrentHashMap<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public List<Task> listar() {
        return new ArrayList<>(tasks.values());
    }

    public Optional<Task> buscar(Long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public Task salvar(Task task) {
        Long id = sequence.incrementAndGet();
        task.setId(id);
        tasks.put(id, task);
        return task;
    }

    public Optional<Task> atualizar(Long id, Task dados) {
        Task atual = tasks.get(id);
        if (atual == null) {
            return Optional.empty();
        }

        atual.setTitulo(dados.getTitulo());
        atual.setDescricao(dados.getDescricao());
        atual.setPrioridade(dados.getPrioridade());
        atual.setConcluida(dados.isConcluida());
        tasks.put(id, atual);

        return Optional.of(atual);
    }

    public boolean deletar(Long id) {
        return tasks.remove(id) != null;
    }
}