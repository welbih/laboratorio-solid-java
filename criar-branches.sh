#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
command -v git >/dev/null 2>&1 || { echo 'Git nao encontrado.' >&2; exit 1; }
if [[ ! -e .git ]]; then
    [[ -n "$(git config user.name || true)" ]] || { echo 'Configure git user.name antes de continuar.' >&2; exit 1; }
    [[ -n "$(git config user.email || true)" ]] || { echo 'Configure git user.email antes de continuar.' >&2; exit 1; }
    git init -b main
fi
[[ -z "$(git rev-parse --show-prefix)" ]] || { echo 'Execute na raiz do projeto.' >&2; exit 1; }
[[ -n "$(git config user.name || true)" ]] || { echo 'Configure git user.name.' >&2; exit 1; }
[[ -n "$(git config user.email || true)" ]] || { echo 'Configure git user.email.' >&2; exit 1; }
git var GIT_AUTHOR_IDENT >/dev/null
git var GIT_COMMITTER_IDENT >/dev/null
if ! git rev-parse --verify HEAD >/dev/null 2>&1; then
    git symbolic-ref HEAD refs/heads/main
    chmod +x -- criar-branches.sh
    git add -- .
    git update-index --chmod=+x -- criar-branches.sh
    git commit -m 'Base legada para laboratorio SOLID'
fi
[[ -z "$(git status --porcelain)" ]] || { echo 'Arvore com alteracoes locais; salve seu trabalho antes de continuar.' >&2; exit 1; }
git symbolic-ref --quiet HEAD >/dev/null || { echo 'HEAD destacado; selecione uma branch.' >&2; exit 1; }
if ! git show-ref --verify --quiet refs/heads/main; then
    git branch main HEAD
fi
git cat-file -e main:NOTAS.md 2>/dev/null && { echo 'main ja contem NOTAS.md; nenhuma branch sera criada.' >&2; exit 1; }
branches=(
    solid-01-responsabilidade-unica
    solid-02-aberto-fechado
    solid-03-substituicao-liskov
    solid-04-segregacao-interfaces
    solid-05-inversao-dependencia
    solid-06-desafio-completo
)
objetivos=(
    'Separar razoes de mudanca preservando a coordenacao do checkout.'
    'Permitir extensao das politicas sem editar continuamente os mesmos componentes.'
    'Rever pre-condicoes, pos-condicoes e invariantes dos contratos de pagamento.'
    'Separar contratos pelas necessidades reais de seus clientes.'
    'Inverter dependencias concretas e manter configuracao padrao na fachada.'
    'Aplicar os cinco principios na mesma base sem merges automaticos.'
)
trap 'git switch --quiet main' EXIT
for indice in "${!branches[@]}"; do
    branch=${branches[$indice]}
    if git show-ref --verify --quiet "refs/heads/$branch"; then
        echo "Preservada: $branch"
        continue
    fi
    git switch --quiet -c "$branch" main
    cat > NOTAS.md <<EOF
# Notas — $branch

## Objetivo da branch
${objetivos[$indice]}

## Problemas identificados

## Classes modificadas

## Decisoes de design

## Refatoracoes realizadas

## Dificuldades encontradas

## Possiveis exageros na aplicacao do principio

## O que seria feito de forma diferente em producao

## Resultado dos testes
EOF
    git add -- NOTAS.md
    git commit --quiet -m "Preparar notas: $branch"
    echo "Criada: $branch"
done
git switch --quiet main
trap - EXIT
