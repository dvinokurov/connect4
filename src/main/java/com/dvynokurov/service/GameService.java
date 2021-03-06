package com.dvynokurov.service;

import com.dvynokurov.model.Game;
import com.dvynokurov.model.GameStatus;
import com.dvynokurov.model.Grid;
import com.dvynokurov.model.Move;
import com.dvynokurov.model.Player;
import com.dvynokurov.repository.GameRepository;
import com.dvynokurov.util.exceptions.GameDoesNotExistException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class GameService {

    @Value("${game.grid.width}")
    private int gridWidth;

    @Value("${game.grid.height}")
    private int gridHeight;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    GridGenerationService gridGenerationService;

    @Autowired
    GridFillingService gridFillingService;

    @Autowired
    AIPlayer aiPlayer;

    @Autowired
    GameFinishedCheckingService gameFinishedCheckingService;

    private ConcurrentMap<UUID, ReentrantLock> gameLocks = new ConcurrentHashMap<>();

    public Game createNewGame(){
        UUID uuid = UUID.randomUUID();
        Grid grid = gridGenerationService.generateGrid(gridWidth, gridHeight);
        Game game = new Game(uuid, grid, GameStatus.IN_PROGRESS);
        gameRepository.save(game);
        gameLocks.put(uuid, new ReentrantLock());
        return game;
    }

    public Game performPlayerMove(UUID gameId, int columnNumber){
        Assert.isTrue(columnNumber >= 0 && columnNumber < gridWidth, "Column number out of range");
        ReentrantLock lock = gameLocks.get(gameId);
        if(lock==null) throw new GameDoesNotExistException();
        try {
            lock.lock();
            return safelyPerformPlayerMove(gameId, columnNumber);
        }finally {
            lock.unlock();
        }
    }

    private Game safelyPerformPlayerMove(UUID gameId, int firstPlayerMove) {
        Game game = gameRepository.findOneById(gameId);
        Grid grid = game.getGrid();
        updateAndPersistGame(firstPlayerMove, game, Player.FIRST);
        if(gameFinishedCheckingService.checkFinished(grid, Player.FIRST)) {
            game.setGameStatus(GameStatus.FIRST_PLAYER_WON);
            gameLocks.remove(gameId);
        }else{
            int secondPlayerMove = aiPlayer.getComputerMove(grid);
            updateAndPersistGame(secondPlayerMove, game, Player.SECOND);
            if (gameFinishedCheckingService.checkFinished(grid, Player.SECOND)) {
                game.setGameStatus(GameStatus.SECOND_PLAYER_WON);
                gameLocks.remove(gameId);
            }
        }
        return game;
    }

    private void updateAndPersistGame(int move, Game game, Player player) {
        final Move moveResult = gridFillingService.putDiskToColumn(game.getGrid(), move, player);
        setLastMove(game, player, moveResult);
        gameRepository.save(game);
    }

    private void setLastMove(Game game, Player player, Move move) {
        if (player.equals(Player.FIRST)) {
            game.setFirstPlayerLastMove(move);
        }else {
            game.setSecondPlayerLastMove(move);
        }
    }

    public Game getGame(UUID gameId) {
        Game oneById = gameRepository.findOneById(gameId);
        if(oneById==null) throw new GameDoesNotExistException();
        return oneById;
    }
}
