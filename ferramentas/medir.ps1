param([switch]$Json)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$fontes = Join-Path $raiz 'src/main/java/br/com/treino/solid'
$arquivos = @(Get-ChildItem -LiteralPath $fontes -Filter '*.java' -Recurse -File | Sort-Object FullName)
$dados = @($arquivos | ForEach-Object {
    $texto = [IO.File]::ReadAllText($_.FullName)
    [PSCustomObject]@{
        Arquivo = $_.FullName.Substring($fontes.Length + 1).Replace('\','/')
        Linhas = @([IO.File]::ReadAllLines($_.FullName)).Count
        MetodosPublicos = [regex]::Matches($texto, '(?m)^    public (?:static )?(?:final )?[\w<>, ?]+\s+\w+\([^;{]*\)\s*\{').Count
        MetodosPrivados = [regex]::Matches($texto, '(?m)^    private [\w<>, ?]+\s+\w+\([^;{]*\)\s*\{').Count
        CondicionaisIf = [regex]::Matches($texto, '\bif\s*\(').Count
        ConstrucoesConcretas = [regex]::Matches($texto, '\bnew\s+[A-Z]\w*\s*(?:<[^>]*>)?\s*\(').Count
        DependenciasConcretas = [regex]::Matches($texto, '(?m)^    private final \w+ \w+ = new \w+\(\);').Count
        MetodosInterface = [regex]::Matches($texto, '(?m)^    (?:void|String|double|boolean) \w+\([^;{}]*\);').Count
        OperacoesNaoSuportadas = [regex]::Matches($texto, 'throw new UnsupportedOperationException\(\)').Count
    }
})
if ($Json) { ConvertTo-Json -InputObject $dados -Depth 4; exit 0 }
$dados | Format-Table -AutoSize
Write-Output ('Arquivos Java de producao: ' + $arquivos.Count)
Write-Output 'Contagens lexicais: incluem bibliotecas e excecoes em new; nao provam conformidade SOLID.'
