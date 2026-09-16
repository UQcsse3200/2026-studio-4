package com.csse3200.game.components.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.FinalBossStageThreeConfig;
import com.csse3200.game.ui.UIComponent;

/** Small ending dialogue, using the existing UI skin. */
public class FinalBossEndingDialogueComponent extends UIComponent {
  private final Entity target;
  private final FinalBossStageThreeConfig config;
  private Table panel;
  private Table jumpHint;
  private Label text;
  private int line;
  private boolean showing;

  public FinalBossEndingDialogueComponent(Entity target, FinalBossStageThreeConfig config) {
    this.target = target;
    this.config = config;
  }

  @Override
  public void create() {
    super.create();
    panel = new Table();
    panel.setFillParent(true);
    panel.bottom().pad(30f);
    text = new Label("", skin);
    text.setWrap(true);
    TextButton next = new TextButton("Continue", skin);
    next.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            advance();
          }
        });
    Table box = new Table();
    box.setBackground(skin.newDrawable("white", 0.05f, 0.06f, 0.12f, 0.95f));
    box.add(text).growX().pad(18f);
    box.row();
    box.add(next).right().pad(12f);
    panel.add(box).growX();
    panel.setVisible(false);
    if (stage != null) stage.addActor(panel);
    jumpHint = new Table();
    jumpHint.setFillParent(true);
    jumpHint.top().padTop(24f);
    jumpHint.add(new Label("SPACE: Jump over the shockwaves", skin)).pad(8f);
    jumpHint.setVisible(false);
    if (stage != null) stage.addActor(jumpHint);
    entity.getEvents().addListener(FinalBossStageThreeComponent.STATE_CHANGED, this::onState);
  }

  private void onState(FinalBossStageThreeState state) {
    if (jumpHint != null) jumpHint.setVisible(state == FinalBossStageThreeState.WAVE_TWO);
    if (state != FinalBossStageThreeState.PEACEFUL || stage == null) return;
    line = 0;
    showing = true;
    text.setText(config.dialogue[line]);
    panel.setVisible(true);
    lock(true);
  }

  @Override
  public void update() {
    if (jumpHint != null) {
      PlayerActions actions = target.getComponent(PlayerActions.class);
      if (actions == null || !actions.isJumpEnabled()) jumpHint.setVisible(false);
    }
    if (showing && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) advance();
  }

  @Override
  protected void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
    // Scene2D draws the dialogue actors.
  }

  private void advance() {
    if (!showing) return;
    line++;
    if (line < config.dialogue.length) text.setText(config.dialogue[line]);
    else {
      showing = false;
      panel.setVisible(false);
      lock(false);
    }
  }

  private void lock(boolean locked) {
    PlayerActions actions = target.getComponent(PlayerActions.class);
    if (actions != null) actions.setControlsLocked(this, locked);
  }

  @Override
  public void dispose() {
    lock(false);
    if (panel != null) panel.remove();
    if (jumpHint != null) jumpHint.remove();
    super.dispose();
  }
}
