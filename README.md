# 1. Visão geral do laboratório

Neste laboratório você vai construir o primeiro microsserviço da solução** MyTracker**: o **`ms-task`**. O objetivo é compreender, na prática, como uma API REST é organizada em uma aplicação Spring Boot.

Nesta primeira versão os dados ficarão **somente em memória**. Ainda não utilizaremos banco de dados, Docker, Kubernetes, mensageria ou segurança. Esses recursos serão incorporados progressivamente nas próximas aulas.

### Ao final você terá

* uma aplicação Spring Boot executando localmente;
* um recurso **`Task`** representando tarefas do MyTracker;
* uma camada **`Service`** com as operações da aplicação;
* uma camada **`Controller`** expondo uma API REST;
* endpoints **`GET`**, **`POST`**, **`PUT`** e **`DELETE`**;
* testes manuais usando IntelliJ HTTP Client, Postman ou Insomnia.

### Arquitetura desta versão

```text
Cliente HTTP
     |
     v
TaskController
     |
     v
 TaskService
     |
     v
List<Task> / memória
```

**Importante:** uma API Spring Boot isolada não caracteriza, por si só, uma arquitetura completa de microsserviços. Hoje construiremos um serviço com uma responsabilidade de negócio bem definida. Nas próximas aulas ele será conteinerizado, distribuído e integrado aos demais componentes da arquitetura Cloud Native.

Se quiser comparar seu projeto com a implementação de referência, acesse: [ms-task no GitLab](https://gitlab.com/gilbriatore/2026/backend/my-tracker/ms-task/-/tree/lab-01).

Para clonar:

```bash
git clone --branch lab-01 https://gitlab.com/gilbriatore/2026/backend/my-tracker/ms-task.git
```

## 2. Definir o contexto e a API

O MyTracker possui diferentes capacidades funcionais: tarefas, hábitos, finanças, dashboard e usuários. Neste laboratório vamos trabalhar apenas com o contexto de** Gestão de Tarefas**.

### Recurso principal

Uma tarefa será representada inicialmente pelos seguintes dados:

```json
{
  "id": 1,
  "titulo": "Estudar Cloud Computing",
  "descricao": "Revisar APIs REST e microsserviços",
  "prioridade": "ALTA",
  "concluida": false
}
```

### Endpoints que serão implementados

* **`GET /tasks`** — listar todas as tarefas;
* **`GET /tasks/{id}`** — consultar uma tarefa;
* **`POST /tasks`** — cadastrar uma tarefa;
* **`PUT /tasks/{id}`** — atualizar uma tarefa;
* **`DELETE /tasks/{id}`** — excluir uma tarefa.

Observe que a URL representa o **recurso** e o método HTTP representa a **operação**. Não criamos URLs como **`/criarTask`** ou **`/deletarTask`**.


## 3. Criar o projeto Spring Boot

No IntelliJ IDEA selecione **New Project → Spring Boot** e utilize o Spring Initializr para criar a aplicação.

### Configuração sugerida

* **Language:** Java
* **Type:** Maven
* **Group:** **`br.mytracker`**
* **Artifact:** **`ms-task`**
* **Package:** **`br.mytracker.mstask`**
* **JDK / Java:** 17 ou superior
* **Packaging:** Jar

### Dependências

Adicione apenas:

* **Spring Web** — criação dos endpoints HTTP;
* **Spring Boot DevTools** — apoio ao desenvolvimento.

O **Spring Boot Starter Test** normalmente já é incluído pelo Initializr.

### O que não adicionar ainda

Não adicione JPA, H2, MySQL, Docker, Kubernetes, RabbitMQ, Spring Security ou qualquer mecanismo de Service Discovery. O objetivo desta aula é manter a solução mínima.


## 4. Organizar os pacotes

Dentro de **`src/main/java/br/mytracker/mstask`**, crie os pacotes abaixo.

```text
br.mytracker.mstask
├── controller
├── domain
├── service
└── repository   # ficará vazio nesta aula
```

A classe **`TaskServiceApplication`** deve permanecer no pacote raiz **`br.mytracker.mstask`**. Dessa forma o Spring encontra automaticamente os componentes definidos nos subpacotes.

### Responsabilidade das camadas

* **domain:** conceitos e objetos do contexto;
* **controller:** entrada e saída HTTP;
* **service:** regras e operações da aplicação;
* **repository:** acesso à persistência — será utilizado em outro momento.


## 5. Criar o domínio `Task`

Crie o arquivo **`src/main/java/br/mytracker/task/domain/Task.java`**.

```java
package br.mytracker.mstask.domain;

public class Task {

    private Long id;
    private String titulo;
    private String descricao;
    private String prioridade;
    private boolean concluida;

    public Task() {
    }

    public Task(Long id, String titulo, String descricao,
                String prioridade, boolean concluida) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.prioridade = prioridade;
        this.concluida = concluida;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public boolean isConcluida() {
        return concluida;
    }

    public void setConcluida(boolean concluida) {
        this.concluida = concluida;
    }
}
```

O Spring/Jackson utilizará os getters e setters para converter automaticamente objetos Java em JSON e JSON em objetos Java.


## 6. Criar o `TaskService`

Crie **`src/main/java/br/mytracker/task/service/TaskService.java`**. Nesta etapa o próprio serviço armazenará as tarefas em memória.

```java
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
```

### Por que `@Service`?

A anotação registra a classe como um componente gerenciado pelo Spring. Assim, **`TaskController`** poderá receber uma instância de **`TaskService`** por injeção de dependência.


## 7. Criar o `TaskController`

Crie **`src/main/java/br/mytracker/task/controller/TaskController.java`**.

```java
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
```

### Anotações importantes

* **`@RestController`** — define um controller REST;
* **`@RequestMapping`** — define o caminho base;
* **`@GetMapping`**, **`@PostMapping`**, **`@PutMapping`** e **`@DeleteMapping`** — associam métodos Java aos verbos HTTP;
* **`@PathVariable`** — lê valores presentes na URL;
* **`@RequestBody`** — converte o JSON recebido em um objeto Java.


## 8. Configurar e executar

Confirme a classe principal gerada pelo Spring Initializr:

```java
package br.mytracker.mstask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
```

Em **`src/main/resources/application.properties`**:

```properties
spring.application.name=ms-task
server.port=8080
```

Execute a classe **`TaskServiceApplication`** pelo IntelliJ. Quando o servidor iniciar, teste no navegador ou cliente HTTP:

```text
http://localhost:8080/tasks
```

Na primeira execução a resposta esperada é **`[]`**, pois nenhuma tarefa foi cadastrada.


## 9. Testar o CRUD REST

Você pode usar o **HTTP Client do IntelliJ**, Postman ou Insomnia. Crie um arquivo **`requests.http`** na raiz do projeto se quiser usar o IntelliJ.

```http
### Listar tarefas
GET http://localhost:8080/tasks
Accept: application/json

### Criar tarefa
POST http://localhost:8080/tasks
Content-Type: application/json

{
  "titulo": "Estudar Cloud Computing",
  "descricao": "Revisar APIs REST e microsserviços",
  "prioridade": "ALTA",
  "concluida": false
}

### Buscar tarefa 1
GET http://localhost:8080/tasks/1
Accept: application/json

### Atualizar tarefa 1
PUT http://localhost:8080/tasks/1
Content-Type: application/json

{
  "titulo": "Estudar Cloud Computing",
  "descricao": "Finalizar o laboratório de API REST",
  "prioridade": "MEDIA",
  "concluida": true
}

### Excluir tarefa 1
DELETE http://localhost:8080/tasks/1
```

### Códigos HTTP esperados

* **200 OK** — consulta ou atualização realizada;
* **201 Created** — tarefa cadastrada;
* **204 No Content** — tarefa excluída;
* **404 Not Found** — ID inexistente.


## 10. Adapte o laboratório ao cenário da sua equipe

Agora não copie o **`ms-task`**. Escolha um contexto funcional do sistema definido pela sua equipe e replique os mesmos conceitos.

### Passo a passo

1. Identifique uma funcionalidade de negócio candidata ao microsserviço.
2. Defina um recurso principal e seus atributos.
3. Defina os endpoints REST do recurso.
4. Crie um projeto Spring Boot independente.
5. Implemente **`domain`**, **`service`** e **`controller`**.
6. Mantenha os objetos em memória.
7. Teste **`GET`**, **`POST`**, **`PUT`** e **`DELETE`**.
8. Versione o código no repositório da equipe.
9. Relacione o trabalho realizado às Tasks técnicas do Azure Boards.

### Checklist de conclusão

* [ ] Aplicação inicia sem erros.
* [ ] O contexto do serviço está claro.
* [ ] GET lista os recursos.
* [ ] POST cria um recurso e retorna 201.
* [ ] PUT atualiza um recurso.
* [ ] DELETE remove um recurso e retorna 204.
* [ ] ID inexistente retorna 404.
* [ ] Código foi versionado.

**Próxima evolução:** na aula de Docker esse mesmo serviço será empacotado em uma imagem e executado em um contêiner. Não descarte o projeto criado hoje.
