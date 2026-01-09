package model;

public enum MessageType {
    ROLE_QUERY, //o content será pedido de role ex "Requesting role"
    ACCUSATION, //o content será o nome do agente acusado espaço motivo ex "AgentX Lying about being Doctor"
    TRUST, //o content será o nome do agente espaço o motivo ex "AgentX Reliable"
    ROLE_CLAIM, //o content será o nome do papel espaço o motivo ex "Doctor Revealing role"
    VOTE, //o content será o nome do agente votado ex "AgentX"
    SYSTEM, //mensagens do sistema, como início e fim de rodada
    DOCTOR_PROTECT, //o content será o nome do agente protegido quando enviado para o GameMaster ex "AgentX"
    SEER_REVEAL, //o content será o nome do agente revelado quando enviado para o GameMaster ex "AgentX"
    SEER_RECEIVE, //o content será o nome do agente espaço a sua role enviado para o Seer ex "AgentX WEREWOLF"
    HUNTER_KILL, //o content será o nome do agente morto quando enviado para o GameMaster ex "AgentX"
    KILL_NOTIFICATION, //o content será o nome do agente morto quando enviado para os agentes ex "AgentX"
    ALIVE_PLAYERS, //o content será a lista de jogadores vivos separados por vírgula ex "AgentX,AgentY,AgentZ"
    WEREWOLF_ATTACK,//o content será o nome do agente escolhido para morrer ex "AgentX"
    WEREWOLF_QUESTION, //o content será a mensagem enviada a perguntar quem atacar
    WEREWOLF_ANSWER //o content será a resposta com o nome do agente a atacar ex "AgentX"
}
