package br.mytracker.mstask.controller;

import br.mytracker.mstask.domain.Task;
import br.mytracker.mstask.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> listar() {
        return taskService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> buscar(@PathVariable Long id) {
        return taskService.buscar(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> salvar(@RequestBody Task task) {
        Task criada = taskService.salvar(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> atualizar(
            @PathVariable Long id,
            @RequestBody Task task) {

        return taskService.atualizar(id, task)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!taskService.deletar(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}