# Village_Mas

Este projeto é uma implementação do **jogo de dedução “A Aldeia”** usando **Java** em um **sistema multiagentes**. Cada aldeão é um agente que interage com os outros, tomando decisões baseadas em confiança e acusações.

## Dependêcias

Antes de executar o projeto, certifique-se de ter:  

- **Biblioteca JSON Object** (para guardar os logs das partidas)  
- **Jade** (Java Agent DEvelopment framework) a framework usada  
- IDE configurada com as bibliotecas adicionadas ao projeto (build path / lib folder)  

## Instalação e Configuração

1. **Adicionar bibliotecas à IDE**  
   - Baixe os `.jar` de **JSON Object** e **Jade**  
   - Adicione-os ao projeto na IDE (pasta `lib` ou build path)  

2. **Verificar dependências**  
   - Todas as dependências devem estar corretamente configuradas para evitar erros na compilação ou execução  

## Execução

O jogo é iniciado através do **GameLauncher.java** e aceita **um argumento**, que é o **número de jogadores.**, caso nenhum argumento seja passado, haverá por padrão 20 jogadores  
A distribuição de roles é feita automáticamente   
O jogo começa e quando termina a partida, é guardado um JSON com os dados da partida

## Dados 
Na pasta stats existe um script python que gera tabelas e plots dos JSONs log das rondas jogadas, as tabelas e plots em png e uma pasta onde os logs são guardados nomeadamente a pasta data
Os dados analisados são:
- ratio de vitória de aldeões e lobisomens
- média de rondas geral e por vitória de aldeões e lobisomens
- percentagem de sobrevivência por role
